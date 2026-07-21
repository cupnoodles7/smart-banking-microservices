package com.smartbank.wallet.service.impl;

import com.smartbank.wallet.client.AccountGateway;
import com.smartbank.wallet.client.TransactionGateway;
import com.smartbank.wallet.client.dto.AccountOperationRequest;
import com.smartbank.wallet.client.dto.RecordTransactionRequest;
import com.smartbank.wallet.constants.FailureReason;
import com.smartbank.wallet.constants.PartyType;
import com.smartbank.wallet.constants.TransactionStatus;
import com.smartbank.wallet.constants.TransactionType;
import com.smartbank.wallet.constants.WalletConstants;
import com.smartbank.wallet.dto.request.CreateWalletRequest;
import com.smartbank.wallet.dto.request.PayBillRequest;
import com.smartbank.wallet.dto.request.TopupRequest;
import com.smartbank.wallet.dto.request.WalletTransferRequest;
import com.smartbank.wallet.dto.response.TransactionResult;
import com.smartbank.wallet.dto.response.WalletResponse;
import com.smartbank.wallet.entity.ProcessedRequest;
import com.smartbank.wallet.entity.Wallet;
import com.smartbank.wallet.exception.BusinessRuleException;
import com.smartbank.wallet.exception.ConcurrentUpdateException;
import com.smartbank.wallet.exception.DuplicateWalletException;
import com.smartbank.wallet.exception.WalletAccessDeniedException;
import com.smartbank.wallet.exception.WalletNotFoundException;
import com.smartbank.wallet.mapper.WalletMapper;
import com.smartbank.wallet.repository.ProcessedRequestRepository;
import com.smartbank.wallet.repository.WalletRepository;
import com.smartbank.wallet.service.WalletService;
import feign.FeignException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Supplier;

// The real wallet logic: creating wallets, moving money with retries and idempotency, and
// undoing half-finished transfers. Business-rule failures come back as a 200 FAILED, not an error.
@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final WalletRepository walletRepository;
    private final ProcessedRequestRepository processedRequestRepository;
    private final AccountGateway accountClient;
    private final TransactionGateway transactionClient;
    private final WalletMapper walletMapper;

    // Wallet money limits, pulled in from central config rather than hard-coded here.
    private final BigDecimal walletMaxBalance;
    private final BigDecimal walletDailyTransferLimit;
    private final int walletDailyTransactionLimit;

    public WalletServiceImpl(WalletRepository walletRepository,
                             ProcessedRequestRepository processedRequestRepository,
                             AccountGateway accountClient,
                             TransactionGateway transactionClient,
                             WalletMapper walletMapper,
                             @Value("${bank.wallet.max-balance}") BigDecimal walletMaxBalance,
                             @Value("${bank.wallet.daily-transfer-limit}") BigDecimal walletDailyTransferLimit,
                             @Value("${bank.wallet.daily-transaction-limit}") int walletDailyTransactionLimit) {
        this.walletRepository = walletRepository;
        this.processedRequestRepository = processedRequestRepository;
        this.accountClient = accountClient;
        this.transactionClient = transactionClient;
        this.walletMapper = walletMapper;
        this.walletMaxBalance = walletMaxBalance;
        this.walletDailyTransferLimit = walletDailyTransferLimit;
        this.walletDailyTransactionLimit = walletDailyTransactionLimit;
    }

    // ------------------------------------------------------------------ create

    @Override
    public WalletResponse createWallet(String customerId, CreateWalletRequest request) {
        if (walletRepository.existsByLinkedAccountId(request.getLinkedAccountId())) {
            throw new DuplicateWalletException(
                    "A wallet already exists for account " + request.getLinkedAccountId());
        }
        LocalDate today = LocalDate.now(ZONE);
        Wallet wallet = Wallet.builder()
                .customerId(customerId)
                .linkedAccountId(request.getLinkedAccountId())
                .walletType(request.getWalletType())
                .balance(BigDecimal.ZERO)
                .maxBalance(walletMaxBalance)
                .dailyTransferLimit(walletDailyTransferLimit)
                .dailyTransferredAmount(BigDecimal.ZERO)
                .dailyTransactionLimit(walletDailyTransactionLimit)
                .todayTransactionCount(0)
                .lastLimitResetDate(today)
                .build();
        try {
            Wallet saved = walletRepository.save(wallet);
            log.info("Wallet created: id={} customerId={} linkedAccountId={} type={}",
                    saved.getId(), customerId, saved.getLinkedAccountId(), saved.getWalletType());
            return walletMapper.toResponse(saved);
        } catch (DuplicateKeyException e) {
            // Lost the race against the unique index on linkedAccountId.
            throw new DuplicateWalletException(
                    "A wallet already exists for account " + request.getLinkedAccountId());
        }
    }

    // ------------------------------------------------------------------- topup

    @Override
    public TransactionResult topup(String customerId, TopupRequest request) {
        Optional<TransactionResult> replay = replay(request.getIdempotencyKey());
        if (replay.isPresent()) {
            return replay.get();
        }

        Wallet wallet = getOwnedWalletOrThrow(request.getWalletId(), customerId);
        boolean accountDebited = false;
        try {
            validateAmountPositive(request.getAmount());
            // Cheap pre-check before touching the account (validate first, then mutate).
            preCheckCredit(wallet, request.getAmount());

            // 1) Take the money out of the linked account first, then top up the wallet.
            //    A successful withdrawal comes back as HTTP 200; if the account says no it's a
            //    400 (FeignException.BadRequest). So a normal return means the money really
            //    moved, and a 400 means nothing left the account.
            try {
                accountClient.withdraw(AccountOperationRequest.builder()
                        .accountId(wallet.getLinkedAccountId())
                        .amount(request.getAmount())
                        .idempotencyKey(request.getIdempotencyKey() + ":acct-debit")
                        .build());
            } catch (FeignException.BadRequest e) {
                // Account rejected the debit (insufficient balance, limit, invalid amount).
                // Nothing was debited, so accountDebited stays false and no refund is needed.
                throw new BusinessRuleException(mapAccountFailure(e),
                        "Account debit rejected: " + e.getMessage());
            }
            // HTTP 200 => the account was really debited; any later failure must refund it.
            accountDebited = true;

            // 2) Credit the wallet (optimistic-locked, retried).
            Wallet updated = withRetry(() -> creditWallet(request.getWalletId(), request.getAmount()));

            TransactionResult result = success(TransactionType.WALLET_TOPUP, request.getAmount(),
                    updated, request.getIdempotencyKey());
            recordLedger(TransactionType.WALLET_TOPUP, PartyType.ACCOUNT, wallet.getLinkedAccountId(),
                    PartyType.WALLET, updated.getId(), result);
            log.info("Top-up SUCCESS: walletId={} amount={} newBalance={}",
                    updated.getId(), request.getAmount(), updated.getBalance());
            return store(request.getIdempotencyKey(), result);

        } catch (BusinessRuleException e) {
            // If we already pulled money from the account, put it back.
            if (accountDebited) {
                compensateAccountCredit(wallet, request.getAmount(), request.getIdempotencyKey());
            }
            return failed(TransactionType.WALLET_TOPUP, request.getAmount(), wallet, e, request.getIdempotencyKey(),
                    PartyType.ACCOUNT, wallet.getLinkedAccountId(), PartyType.WALLET, wallet.getId());
        } catch (ConcurrentUpdateException e) {
            if (accountDebited) {
                compensateAccountCredit(wallet, request.getAmount(), request.getIdempotencyKey());
            }
            throw e;
        }
    }

    // ---------------------------------------------------------------- transfer

    @Override
    public TransactionResult transfer(String customerId, WalletTransferRequest request) {
        Optional<TransactionResult> replay = replay(request.getIdempotencyKey());
        if (replay.isPresent()) {
            return replay.get();
        }

        // Caller must own the source wallet; the destination may belong to another
        // customer (you can transfer money to someone else), so it need only exist.
        Wallet from = getOwnedWalletOrThrow(request.getFromWalletId(), customerId);
        Wallet to = getWalletOrThrow(request.getToWalletId());
        try {
            if (from.getId().equals(to.getId())) {
                throw new BusinessRuleException(FailureReason.SELF_TRANSFER,
                        "Sender and receiver wallet cannot be the same");
            }
            validateAmountPositive(request.getAmount());
            // Pre-check destination capacity to avoid a needless debit+reverse in the common case.
            preCheckCredit(to, request.getAmount());

            // 1) Debit source (optimistic-locked, retried; re-validates balance & limits).
            Wallet debited = withRetry(() -> debitWallet(request.getFromWalletId(), request.getAmount()));

            // 2) Add it to the destination; if that fails, put it back on the source.
            Wallet credited;
            try {
                credited = withRetry(() -> creditWallet(request.getToWalletId(), request.getAmount()));
            } catch (BusinessRuleException | ConcurrentUpdateException e) {
                reverseDebit(request.getFromWalletId(), request.getAmount());
                throw e;
            }

            TransactionResult result = success(TransactionType.WALLET_TRANSFER, request.getAmount(),
                    debited, request.getIdempotencyKey());
            recordLedger(TransactionType.WALLET_TRANSFER, PartyType.WALLET, debited.getId(),
                    PartyType.WALLET, credited.getId(), result);
            log.info("Wallet transfer SUCCESS: from={} to={} amount={}",
                    debited.getId(), credited.getId(), request.getAmount());
            return store(request.getIdempotencyKey(), result);

        } catch (BusinessRuleException e) {
            return failed(TransactionType.WALLET_TRANSFER, request.getAmount(), from, e, request.getIdempotencyKey(),
                    PartyType.WALLET, from.getId(), PartyType.WALLET, to.getId());
        }
        // A ConcurrentUpdateException bubbles up as a 409 (any debit has already been undone above).
    }

    // ---------------------------------------------------------------- pay bill

    @Override
    public TransactionResult payBill(String customerId, PayBillRequest request) {
        Optional<TransactionResult> replay = replay(request.getIdempotencyKey());
        if (replay.isPresent()) {
            return replay.get();
        }

        Wallet wallet = getOwnedWalletOrThrow(request.getWalletId(), customerId);
        try {
            validateAmountPositive(request.getAmount());
            // Debit the wallet; receiver is an external MERCHANT, so nothing to credit.
            Wallet debited = withRetry(() -> debitWallet(request.getWalletId(), request.getAmount()));

            TransactionResult result = success(TransactionType.BILL_PAYMENT, request.getAmount(),
                    debited, request.getIdempotencyKey());
            recordLedger(TransactionType.BILL_PAYMENT, PartyType.WALLET, debited.getId(),
                    PartyType.MERCHANT, request.getBillerId(), result);
            log.info("Bill paid: walletId={} billerId={} amount={} newBalance={}",
                    debited.getId(), request.getBillerId(), request.getAmount(), debited.getBalance());
            return store(request.getIdempotencyKey(), result);

        } catch (BusinessRuleException e) {
            return failed(TransactionType.BILL_PAYMENT, request.getAmount(), wallet, e, request.getIdempotencyKey(),
                    PartyType.WALLET, wallet.getId(), PartyType.MERCHANT, request.getBillerId());
        }
    }

    // -------------------------------------------------------------------- list

    @Override
    public Page<WalletResponse> listByCustomer(String callerCustomerId, String targetCustomerId, Pageable pageable) {
        // You can only list your own wallets.
        assertSelf(callerCustomerId, targetCustomerId);
        return walletRepository.findByCustomerId(targetCustomerId, pageable).map(walletMapper::toResponse);
    }

    // =============================================================== internals

    // Take money out of a wallet: grab a fresh copy, roll over the daily counters if it's a new day,
    // make sure the balance and daily limits are OK, then save. If a rule is broken we throw and
    // change nothing. The save can lose a race, in which case the caller retries.
    private Wallet debitWallet(String walletId, BigDecimal amount) {
        Wallet wallet = getWalletOrThrow(walletId);
        applyLazyReset(wallet);

        if (wallet.getTodayTransactionCount() >= wallet.getDailyTransactionLimit()) {
            log.warn("Daily transaction count reached: walletId={}", walletId);
            throw new BusinessRuleException(FailureReason.DAILY_LIMIT_EXCEEDED,
                    "Daily transaction count limit reached");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance: walletId={} balance={} requested={}",
                    walletId, wallet.getBalance(), amount);
            throw new BusinessRuleException(FailureReason.INSUFFICIENT_BALANCE,
                    "Insufficient wallet balance");
        }
        BigDecimal transferredAfter = wallet.getDailyTransferredAmount().add(amount);
        if (transferredAfter.compareTo(wallet.getDailyTransferLimit()) > 0) {
            log.warn("Daily transfer amount limit reached: walletId={} alreadyTransferred={} requested={}",
                    walletId, wallet.getDailyTransferredAmount(), amount);
            throw new BusinessRuleException(FailureReason.DAILY_LIMIT_EXCEEDED,
                    "Daily transfer amount limit exceeded");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setDailyTransferredAmount(transferredAfter);
        wallet.setTodayTransactionCount(wallet.getTodayTransactionCount() + 1);
        return walletRepository.save(wallet);
    }

    // Add money to a wallet: grab a fresh copy, roll over the daily counters if needed, check we
    // won't blow past the max balance, then save. Money coming in doesn't count against the daily
    // transfer limit.
    private Wallet creditWallet(String walletId, BigDecimal amount) {
        Wallet wallet = getWalletOrThrow(walletId);
        applyLazyReset(wallet);

        if (wallet.getBalance().add(amount).compareTo(wallet.getMaxBalance()) > 0) {
            log.warn("Wallet limit reached: walletId={} balance={} incoming={} max={}",
                    walletId, wallet.getBalance(), amount, wallet.getMaxBalance());
            throw new BusinessRuleException(FailureReason.WALLET_LIMIT_EXCEEDED,
                    "Wallet balance would exceed the ₹50,000 limit");
        }
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTodayTransactionCount(wallet.getTodayTransactionCount() + 1);
        return walletRepository.save(wallet);
    }

    // Put money back on the source wallet after the other side of a transfer failed. We retry;
    // if it still won't go through, we log an error so someone can sort it out by hand.
    private void reverseDebit(String walletId, BigDecimal amount) {
        try {
            withRetry(() -> {
                Wallet wallet = getWalletOrThrow(walletId);
                wallet.setBalance(wallet.getBalance().add(amount));
                wallet.setDailyTransferredAmount(
                        wallet.getDailyTransferredAmount().subtract(amount).max(BigDecimal.ZERO));
                wallet.setTodayTransactionCount(Math.max(0, wallet.getTodayTransactionCount() - 1));
                return walletRepository.save(wallet);
            });
            log.info("Reversal applied: walletId={} amount={} refunded", walletId, amount);
        } catch (RuntimeException e) {
            log.error("REVERSAL FAILED — manual reconciliation required: walletId={} amount={} cause={}",
                    walletId, amount, e.getMessage(), e);
        }
    }

    // Refund the linked account when a top-up took the money out but couldn't credit the wallet.
    // We try a few times and log an error if it never succeeds.
    private void compensateAccountCredit(Wallet wallet, BigDecimal amount, String idempotencyKey) {
        for (int attempt = 1; attempt <= WalletConstants.MAX_REVERSAL_RETRIES; attempt++) {
            try {
                // A normal return is HTTP 200 => the credit was applied. A business rejection
                // (HTTP 400) or any transport error throws and is retried below.
                accountClient.deposit(AccountOperationRequest.builder()
                        .accountId(wallet.getLinkedAccountId())
                        .amount(amount)
                        .idempotencyKey(idempotencyKey + ":acct-refund")
                        .build());
                log.info("Account refund applied: accountId={} amount={}", wallet.getLinkedAccountId(), amount);
                return;
            } catch (RuntimeException e) {
                log.warn("Account refund attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        log.error("ACCOUNT REFUND FAILED — manual reconciliation required: accountId={} amount={}",
                wallet.getLinkedAccountId(), amount);
    }

    // If it's a new day, zero out the daily counters. Done in memory here and saved on the next write.
    private void applyLazyReset(Wallet wallet) {
        LocalDate today = LocalDate.now(ZONE);
        if (!today.equals(wallet.getLastLimitResetDate())) {
            wallet.setTodayTransactionCount(0);
            wallet.setDailyTransferredAmount(BigDecimal.ZERO);
            wallet.setLastLimitResetDate(today);
        }
    }

    // A quick look-but-don't-touch check that the incoming money won't push the wallet over its cap.
    private void preCheckCredit(Wallet wallet, BigDecimal amount) {
        if (amount != null && amount.signum() > 0
                && wallet.getBalance().add(amount).compareTo(wallet.getMaxBalance()) > 0) {
            throw new BusinessRuleException(FailureReason.WALLET_LIMIT_EXCEEDED,
                    "Wallet balance would exceed the ₹50,000 limit");
        }
    }

    private void validateAmountPositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException(FailureReason.INVALID_AMOUNT,
                    "Amount must be greater than zero");
        }
    }

    private Wallet getWalletOrThrow(String walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }

    // Fetch a wallet and make sure the logged-in customer owns it. Every operation starts here so
    // nobody can touch someone else's wallet. No such wallet is a 404; wrong owner is a 403.
    private Wallet getOwnedWalletOrThrow(String walletId, String customerId) {
        Wallet wallet = getWalletOrThrow(walletId);
        assertOwnership(wallet, customerId);
        return wallet;
    }

    // Turn the request away with a 403 unless this customer actually owns the wallet.
    private void assertOwnership(Wallet wallet, String customerId) {
        if (customerId == null || !customerId.equals(wallet.getCustomerId())) {
            log.warn("Ownership check failed: customerId={} does not own walletId={} (owner={})",
                    customerId, wallet.getId(), wallet.getCustomerId());
            throw new WalletAccessDeniedException(
                    "You do not have access to wallet " + wallet.getId());
        }
    }

    // Turn the request away with a 403 unless the caller is asking about their own customer id.
    private void assertSelf(String callerCustomerId, String targetCustomerId) {
        if (callerCustomerId == null || !callerCustomerId.equals(targetCustomerId)) {
            log.warn("Customer {} attempted to list wallets of {}", callerCustomerId, targetCustomerId);
            throw new WalletAccessDeniedException("You may only access your own wallets");
        }
    }

    // Retry a balance-changing save when another update beat us to it, up to a few attempts.
    private <T> T withRetry(Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            try {
                return operation.get();
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= WalletConstants.MAX_WRITE_RETRIES) {
                    throw new ConcurrentUpdateException(
                            "Could not complete the write after " + attempt
                                    + " attempts due to concurrent modification");
                }
                log.warn("Optimistic-lock conflict (attempt {}/{}), retrying",
                        attempt, WalletConstants.MAX_WRITE_RETRIES);
            }
        }
    }

    // ----------------------------------------------------------- idempotency

    private Optional<TransactionResult> replay(String idempotencyKey) {
        return processedRequestRepository.findById(idempotencyKey)
                .map(pr -> {
                    log.info("Idempotent replay for key={} — returning stored result", idempotencyKey);
                    return pr.getResult();
                });
    }

    private TransactionResult store(String idempotencyKey, TransactionResult result) {
        try {
            processedRequestRepository.save(ProcessedRequest.builder()
                    .idempotencyKey(idempotencyKey)
                    .result(result)
                    .createdAt(Instant.now())
                    .build());
        } catch (DuplicateKeyException e) {
            // A concurrent request with the same key already stored a result; return it.
            return replay(idempotencyKey).orElse(result);
        }
        return result;
    }

    // ------------------------------------------------------------ result build

    private TransactionResult success(TransactionType type, BigDecimal amount, Wallet source, String key) {
        return TransactionResult.builder()
                .status(TransactionStatus.SUCCESS)
                .failureReason(FailureReason.NONE)
                .transactionType(type)
                .amount(amount)
                .walletId(source.getId())
                .resultingBalance(source.getBalance())
                .idempotencyKey(key)
                .message("Operation completed successfully")
                .timestamp(Instant.now())
                .build();
    }

    private TransactionResult failed(TransactionType type, BigDecimal amount, Wallet source,
                                     BusinessRuleException ex, String key,
                                     PartyType senderType, String senderId,
                                     PartyType receiverType, String receiverId) {
        TransactionResult result = TransactionResult.builder()
                .status(TransactionStatus.FAILED)
                .failureReason(ex.getFailureReason())
                .transactionType(type)
                .amount(amount)
                .walletId(source.getId())
                .resultingBalance(source.getBalance())
                .idempotencyKey(key)
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        // We record failed transactions too, not just the ones that went through.
        recordLedger(type, senderType, senderId, receiverType, receiverId, result);
        return store(key, result);
    }

    // Write the transaction to the ledger, best-effort. If it fails after money has already moved,
    // we log an error but still tell the client it worked - we never undo money that's settled.
    private void recordLedger(TransactionType type, PartyType senderType, String senderId,
                              PartyType receiverType, String receiverId, TransactionResult result) {
        try {
            transactionClient.record(RecordTransactionRequest.builder()
                    .transactionType(type)
                    .senderType(senderType)
                    .senderId(senderId)
                    .receiverType(receiverType)
                    .receiverId(receiverId)
                    .amount(result.getAmount())
                    .currency(WalletConstants.CURRENCY)
                    .status(result.getStatus())
                    .failureReason(result.getFailureReason())
                    .idempotencyKey(result.getIdempotencyKey())
                    .initiatedAt(result.getTimestamp())
                    .completedAt(Instant.now())
                    .build());
        } catch (RuntimeException e) {
            log.error("Ledger write failed (money already moved for {} {}): {}",
                    type, result.getStatus(), e.getMessage());
        }
    }

    // Work out why the account said no to a withdrawal. Its error only gives us a message to read,
    // so we look for familiar words and default to "not enough money" - the usual reason.
    private FailureReason mapAccountFailure(FeignException e) {
        String body = e.contentUTF8();
        String text = body == null ? "" : body.toLowerCase(java.util.Locale.ROOT);
        if (text.contains("insufficient")) {
            return FailureReason.INSUFFICIENT_BALANCE;
        }
        if (text.contains("limit")) {
            return FailureReason.DAILY_LIMIT_EXCEEDED;
        }
        if (text.contains("amount")) {
            return FailureReason.INVALID_AMOUNT;
        }
        if (text.contains("account")) {
            return FailureReason.INVALID_ACCOUNT;
        }
        return FailureReason.INSUFFICIENT_BALANCE;
    }
}
