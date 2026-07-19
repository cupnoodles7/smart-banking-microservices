package com.smartbank.wallet.service.impl;

import com.smartbank.wallet.client.AccountClient;
import com.smartbank.wallet.client.TransactionClient;
import com.smartbank.wallet.client.dto.AccountOperationRequest;
import com.smartbank.wallet.client.dto.AccountOperationResponse;
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
import com.smartbank.wallet.exception.WalletNotFoundException;
import com.smartbank.wallet.mapper.WalletMapper;
import com.smartbank.wallet.repository.ProcessedRequestRepository;
import com.smartbank.wallet.repository.WalletRepository;
import com.smartbank.wallet.service.WalletService;
import org.slf4j.Logger;
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

/**
 * Wallet business logic (PRD §6). Implements lazy counter reset (§6.14), optimistic
 * locking with bounded retry (§6.15), idempotency (§6.15) and synchronous
 * compensating reversals for partial multi-owner failures (§6.16).
 *
 * <p>Convention (PRD §7.3): business-rule violations are returned as HTTP 200 with a
 * FAILED {@link TransactionResult}; only structural problems (missing wallet, lock
 * conflict exhausted) throw and become §6.9 error responses.
 */
@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final WalletRepository walletRepository;
    private final ProcessedRequestRepository processedRequestRepository;
    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    private final WalletMapper walletMapper;

    public WalletServiceImpl(WalletRepository walletRepository,
                             ProcessedRequestRepository processedRequestRepository,
                             AccountClient accountClient,
                             TransactionClient transactionClient,
                             WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.processedRequestRepository = processedRequestRepository;
        this.accountClient = accountClient;
        this.transactionClient = transactionClient;
        this.walletMapper = walletMapper;
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
                .maxBalance(WalletConstants.MAX_BALANCE)
                .dailyTransferLimit(WalletConstants.DAILY_TRANSFER_LIMIT)
                .dailyTransferredAmount(BigDecimal.ZERO)
                .dailyTransactionLimit(WalletConstants.DAILY_TRANSACTION_LIMIT)
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

        Wallet wallet = getWalletOrThrow(request.getWalletId());
        boolean accountDebited = false;
        try {
            validateAmountPositive(request.getAmount());
            // Cheap pre-check before touching the account (validate first, then mutate).
            preCheckCredit(wallet, request.getAmount());

            // 1) Debit the linked account first (PRD §6.16: debit source, then credit).
            AccountOperationResponse debit = accountClient.withdraw(AccountOperationRequest.builder()
                    .accountId(wallet.getLinkedAccountId())
                    .amount(request.getAmount())
                    .idempotencyKey(request.getIdempotencyKey() + ":acct-debit")
                    .build());
            if (!debit.isSuccess()) {
                throw new BusinessRuleException(mapAccountFailure(debit.getFailureReason()),
                        "Account debit failed: " + debit.getFailureReason());
            }
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
            // If the account was already debited, refund it (compensating reversal, §6.16).
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

        Wallet from = getWalletOrThrow(request.getFromWalletId());
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

            // 2) Credit destination; on failure, reverse the source debit (§6.16).
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
        // ConcurrentUpdateException propagates → HTTP 409 (money already reversed if debited).
    }

    // ---------------------------------------------------------------- pay bill

    @Override
    public TransactionResult payBill(String customerId, PayBillRequest request) {
        Optional<TransactionResult> replay = replay(request.getIdempotencyKey());
        if (replay.isPresent()) {
            return replay.get();
        }

        Wallet wallet = getWalletOrThrow(request.getWalletId());
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
    public Page<WalletResponse> listByCustomer(String customerId, Pageable pageable) {
        return walletRepository.findByCustomerId(customerId, pageable).map(walletMapper::toResponse);
    }

    // =============================================================== internals

    /**
     * Debit a wallet: re-read fresh, lazy-reset counters, validate balance and daily
     * limits, then mutate. Throws {@link BusinessRuleException} on any rule violation
     * (no state is persisted in that case). Save may raise
     * {@link OptimisticLockingFailureException}, retried by the caller.
     */
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

    /**
     * Credit a wallet: re-read fresh, lazy-reset, validate the ₹50,000 cap, then
     * mutate. Incoming money does not count against the daily transfer amount.
     */
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

    /**
     * Reverse a source debit after a failed downstream credit (§6.16). Retried; if it
     * ultimately fails the discrepancy is logged at ERROR for manual reconciliation.
     */
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

    /**
     * Refund the linked account after a failed wallet top-up credit (§6.16).
     * Best-effort with logging; not retried at the Feign layer beyond one attempt here.
     */
    private void compensateAccountCredit(Wallet wallet, BigDecimal amount, String idempotencyKey) {
        for (int attempt = 1; attempt <= WalletConstants.MAX_REVERSAL_RETRIES; attempt++) {
            try {
                AccountOperationResponse res = accountClient.deposit(AccountOperationRequest.builder()
                        .accountId(wallet.getLinkedAccountId())
                        .amount(amount)
                        .idempotencyKey(idempotencyKey + ":acct-refund")
                        .build());
                if (res.isSuccess()) {
                    log.info("Account refund applied: accountId={} amount={}", wallet.getLinkedAccountId(), amount);
                    return;
                }
                log.warn("Account refund attempt {} returned {}", attempt, res.getFailureReason());
            } catch (RuntimeException e) {
                log.warn("Account refund attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        log.error("ACCOUNT REFUND FAILED — manual reconciliation required: accountId={} amount={}",
                wallet.getLinkedAccountId(), amount);
    }

    /** Lazy daily-counter reset (PRD §6.14). Mutates in-memory; persisted on next save. */
    private void applyLazyReset(Wallet wallet) {
        LocalDate today = LocalDate.now(ZONE);
        if (!today.equals(wallet.getLastLimitResetDate())) {
            wallet.setTodayTransactionCount(0);
            wallet.setDailyTransferredAmount(BigDecimal.ZERO);
            wallet.setLastLimitResetDate(today);
        }
    }

    /** Read-only pre-check of the ₹50,000 cap on a credit target. */
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

    /** Retry a balance-mutating write on optimistic-lock conflict (PRD §6.15). */
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
        // Failed transactions are persisted too (PRD §6.6).
        recordLedger(type, senderType, senderId, receiverType, receiverId, result);
        return store(key, result);
    }

    /**
     * Best-effort ledger write (PRD §6.16): if it fails after money already moved, log
     * at ERROR and still report success to the client — never roll back settled money.
     */
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

    private FailureReason mapAccountFailure(String reason) {
        if (reason == null) {
            return FailureReason.INVALID_ACCOUNT;
        }
        try {
            return FailureReason.valueOf(reason);
        } catch (IllegalArgumentException e) {
            return FailureReason.INSUFFICIENT_BALANCE;
        }
    }
}
