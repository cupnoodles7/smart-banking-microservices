package com.smartbank.account.service.impl;

import com.smartbank.account.dto.request.CreateAccountRequest;
import com.smartbank.account.dto.request.DepositRequest;
import com.smartbank.account.dto.request.TransferRequest;
import com.smartbank.account.dto.request.WithdrawRequest;
import com.smartbank.account.dto.response.AccountResponse;
import com.smartbank.account.entity.Account;
import com.smartbank.account.entity.AccountType;
import com.smartbank.account.entity.FailureReason;
import com.smartbank.account.entity.ProcessedOperation;
import com.smartbank.account.entity.TransactionType;
import com.smartbank.account.exception.AccountNotFoundException;
import com.smartbank.account.exception.CustomerNotFoundException;
import com.smartbank.account.exception.DailyLimitExceededException;
import com.smartbank.account.exception.InsufficientBalanceException;
import com.smartbank.account.exception.InvalidAccountException;
import com.smartbank.account.exception.InvalidAmountException;
import com.smartbank.account.exception.SelfTransferException;
import com.smartbank.account.exception.UnauthorizedAccountAccessException;
import com.smartbank.account.exception.UserServiceUnavailableException;
import com.smartbank.account.repository.AccountRepository;
import com.smartbank.account.repository.ProcessedOperationRepository;
import com.smartbank.account.service.AccountService;
import com.smartbank.account.util.DailyLimitResetEvaluator;
import com.smartbank.account.util.TransactionRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final String CUSTOMER_ID_HEADER = "X-Customer-Id";

    private final AccountRepository repository;
    private final ProcessedOperationRepository processedOperationRepository;
    private final RestTemplate restTemplate;
    private final TransactionRecorder transactionRecorder;
    private final boolean userValidationEnabled;
    private final BigDecimal savingsMaxBalance;
    private final BigDecimal savingsDailyTransferLimit;
    private final int savingsDailyTransactionLimit;
    private final BigDecimal currentMaxBalance;
    private final BigDecimal currentDailyTransferLimit;
    private final int currentDailyTransactionLimit;

    public AccountServiceImpl(
            AccountRepository repository,
            ProcessedOperationRepository processedOperationRepository,
            RestTemplate restTemplate,
            TransactionRecorder transactionRecorder,
            @Value("${user-service.validation-enabled:true}") boolean userValidationEnabled,
            @Value("${bank.savings.max-balance}") BigDecimal savingsMaxBalance,
            @Value("${bank.savings.daily-transfer-limit}") BigDecimal savingsDailyTransferLimit,
            @Value("${bank.savings.daily-transaction-limit}") int savingsDailyTransactionLimit,
            @Value("${bank.current.max-balance}") BigDecimal currentMaxBalance,
            @Value("${bank.current.daily-transfer-limit}") BigDecimal currentDailyTransferLimit,
            @Value("${bank.current.daily-transaction-limit}") int currentDailyTransactionLimit) {
        this.repository = repository;
        this.processedOperationRepository = processedOperationRepository;
        this.restTemplate = restTemplate;
        this.transactionRecorder = transactionRecorder;
        this.userValidationEnabled = userValidationEnabled;
        this.savingsMaxBalance = savingsMaxBalance;
        this.savingsDailyTransferLimit = savingsDailyTransferLimit;
        this.savingsDailyTransactionLimit = savingsDailyTransactionLimit;
        this.currentMaxBalance = currentMaxBalance;
        this.currentDailyTransferLimit = currentDailyTransferLimit;
        this.currentDailyTransactionLimit = currentDailyTransactionLimit;
    }

    @Override
    public AccountResponse createAccount(String callerCustomerId, CreateAccountRequest request) {
        requireCallerId(callerCustomerId);
        validateCustomerExists(callerCustomerId);

        LocalDateTime now = LocalDateTime.now();
        Account account = Account.builder()
                .customerId(callerCustomerId)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .maxBalance(maxBalanceFor(request.getAccountType()))
                .dailyTransferLimit(dailyTransferLimitFor(request.getAccountType()))
                .dailyTransactionLimit(dailyTransactionLimitFor(request.getAccountType()))
                .dailyTransferredAmount(BigDecimal.ZERO)
                .todayTransactionCount(0)
                .lastLimitResetDate(LocalDate.now())
                .currency("INR")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account saved = repository.save(account);
        log.info("Created {} account {} for customer {}", saved.getAccountType(), saved.getId(), callerCustomerId);
        return AccountResponse.from(saved);
    }

    @Override
    public List<AccountResponse> getByCustomerId(String customerId, String callerCustomerId) {
        requireCallerId(callerCustomerId);
        if (!customerId.equals(callerCustomerId)) {
            log.warn("Customer {} attempted to list accounts belonging to {}", callerCustomerId, customerId);
            throw new UnauthorizedAccountAccessException("You may only view your own accounts");
        }
        log.info("Retrieving accounts for customer {}", customerId);
        return repository.findByCustomerId(customerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Override
    public AccountResponse deposit(String callerCustomerId, DepositRequest request) {
        requireCallerId(callerCustomerId);

        // Idempotency: if this exact operation was already applied, replay the stored result instead
        // of crediting again (protects against Wallet Service refund retries after a timeout).
        Optional<AccountResponse> replay = replayIfProcessed(request.getIdempotencyKey());
        if (replay.isPresent()) {
            log.info("Idempotent replay - deposit key={} already applied, not crediting again",
                    request.getIdempotencyKey());
            return replay.get();
        }

        Account account = loadOwnedAccount(request.getAccountId(), callerCustomerId);
        BigDecimal amount = request.getAmount();

        if (isNotPositive(amount)) {
            log.warn("Invalid deposit amount {} for account {}", amount, account.getId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.DEPOSIT,
                    account.getId(), account.getId(), amount, FailureReason.INVALID_AMOUNT);
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        DailyLimitResetEvaluator.resetIfNewDay(account);

        BigDecimal newBalance = account.getBalance().add(amount);
        if (newBalance.compareTo(account.getMaxBalance()) > 0) {
            log.warn("Deposit of {} would exceed max balance {} for account {}",
                    amount, account.getMaxBalance(), account.getId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.DEPOSIT,
                    account.getId(), account.getId(), amount, FailureReason.INVALID_AMOUNT);
            throw new InvalidAmountException("Deposit would exceed the account's maximum balance");
        }

        account.setBalance(newBalance);
        account.setTodayTransactionCount(account.getTodayTransactionCount() + 1);
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = repository.save(account);

        log.info("Deposited {} into account {}", amount, saved.getId());
        AccountResponse response = AccountResponse.from(saved);
        rememberProcessed(request.getIdempotencyKey(), response);
        transactionRecorder.recordDeposit(callerCustomerId, saved.getId(), amount, saved.getUpdatedAt());
        return response;
    }

    @Override
    public AccountResponse withdraw(String callerCustomerId, WithdrawRequest request) {
        requireCallerId(callerCustomerId);

        // Idempotency: if this exact operation was already applied, replay the stored result instead
        // of debiting again (protects against retries after a timeout).
        Optional<AccountResponse> replay = replayIfProcessed(request.getIdempotencyKey());
        if (replay.isPresent()) {
            log.info("Idempotent replay - withdraw key={} already applied, not debiting again",
                    request.getIdempotencyKey());
            return replay.get();
        }

        Account account = loadOwnedAccount(request.getAccountId(), callerCustomerId);
        BigDecimal amount = request.getAmount();

        if (isNotPositive(amount)) {
            log.warn("Invalid withdrawal amount {} for account {}", amount, account.getId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.WITHDRAW,
                    account.getId(), account.getId(), amount, FailureReason.INVALID_AMOUNT);
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        DailyLimitResetEvaluator.resetIfNewDay(account);
        applyOutgoingLimitChecks(callerCustomerId, TransactionType.WITHDRAW, account, amount);

        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for withdrawal of {} from account {}", amount, account.getId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.WITHDRAW,
                    account.getId(), account.getId(), amount, FailureReason.INSUFFICIENT_BALANCE);
            throw new InsufficientBalanceException("Insufficient balance for this withdrawal");
        }

        debit(account, amount);
        Account saved = repository.save(account);

        log.info("Withdrew {} from account {}", amount, saved.getId());
        AccountResponse response = AccountResponse.from(saved);
        rememberProcessed(request.getIdempotencyKey(), response);
        transactionRecorder.recordWithdraw(callerCustomerId, saved.getId(), amount, saved.getUpdatedAt());
        return response;
    }

    // Returns the stored result if this idempotency key has already been applied, else empty.
    // A blank/absent key means the caller opted out of deduplication.
    private Optional<AccountResponse> replayIfProcessed(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return Optional.empty();
        }
        return processedOperationRepository.findById(idempotencyKey).map(ProcessedOperation::getResult);
    }

    // Records that this idempotency key has been applied, so a later retry replays the result.
    // insert() (not save) makes the @Id a real uniqueness guard; a concurrent duplicate is harmless.
    private void rememberProcessed(String idempotencyKey, AccountResponse result) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        try {
            processedOperationRepository.insert(ProcessedOperation.builder()
                    .idempotencyKey(idempotencyKey)
                    .accountId(result.getId())
                    .result(result)
                    .createdAt(Instant.now())
                    .build());
        } catch (DuplicateKeyException ex) {
            log.warn("Idempotency key {} was already recorded by a concurrent request - ignoring",
                    idempotencyKey);
        }
    }

    @Override
    @Transactional
    public AccountResponse transfer(String callerCustomerId, TransferRequest request) {
        requireCallerId(callerCustomerId);

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            log.warn("Self-transfer blocked for account {}", request.getFromAccountId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.TRANSFER,
                    request.getFromAccountId(), request.getToAccountId(), request.getAmount(),
                    FailureReason.SELF_TRANSFER);
            throw new SelfTransferException("Sender and receiver accounts must differ");
        }

        Account fromAccount = loadOwnedAccount(request.getFromAccountId(), callerCustomerId);

        Account toAccount = repository.findById(request.getToAccountId())
                .orElseThrow(() -> {
                    log.warn("Transfer target account {} does not exist", request.getToAccountId());
                    transactionRecorder.recordFailure(callerCustomerId, TransactionType.TRANSFER,
                            fromAccount.getId(), request.getToAccountId(), request.getAmount(),
                            FailureReason.INVALID_ACCOUNT);
                    return new InvalidAccountException("Target account does not exist");
                });

        BigDecimal amount = request.getAmount();
        if (isNotPositive(amount)) {
            log.warn("Invalid transfer amount {} from account {}", amount, fromAccount.getId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.TRANSFER,
                    fromAccount.getId(), toAccount.getId(), amount, FailureReason.INVALID_AMOUNT);
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        DailyLimitResetEvaluator.resetIfNewDay(fromAccount);
        DailyLimitResetEvaluator.resetIfNewDay(toAccount);
        applyOutgoingLimitChecks(callerCustomerId, TransactionType.TRANSFER, fromAccount, amount, toAccount.getId());

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for transfer of {} from account {}", amount, fromAccount.getId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.TRANSFER,
                    fromAccount.getId(), toAccount.getId(), amount, FailureReason.INSUFFICIENT_BALANCE);
            throw new InsufficientBalanceException("Insufficient balance for this transfer");
        }

        BigDecimal creditedBalance = toAccount.getBalance().add(amount);
        if (creditedBalance.compareTo(toAccount.getMaxBalance()) > 0) {
            log.warn("Transfer of {} would exceed max balance {} for target account {}",
                    amount, toAccount.getMaxBalance(), toAccount.getId());
            transactionRecorder.recordFailure(callerCustomerId, TransactionType.TRANSFER,
                    fromAccount.getId(), toAccount.getId(), amount, FailureReason.INVALID_ACCOUNT);
            throw new InvalidAccountException("Transfer would exceed the target account's maximum balance");
        }

        debit(fromAccount, amount);
        toAccount.setBalance(creditedBalance);
        toAccount.setTodayTransactionCount(toAccount.getTodayTransactionCount() + 1);
        toAccount.setUpdatedAt(LocalDateTime.now());

        Account savedFrom = repository.save(fromAccount);
        repository.save(toAccount);

        log.info("Transferred {} from account {} to account {}", amount, savedFrom.getId(), toAccount.getId());
        transactionRecorder.recordTransfer(callerCustomerId, savedFrom.getId(), toAccount.getId(),
                amount, savedFrom.getUpdatedAt());
        return AccountResponse.from(savedFrom);
    }
    private void requireCallerId(String callerCustomerId) {
        if (!StringUtils.hasText(callerCustomerId)) {
            throw new UnauthorizedAccountAccessException("Missing caller identity (X-Customer-Id)");
        }
    }
    private void validateCustomerExists(String callerCustomerId) {
        if (!userValidationEnabled) {
            log.warn("Skipping User Service validation for customer {} - "
                    + "user-service.validation-enabled=false", callerCustomerId);
            return;
        }
        String url = "http://user-service/users/" + callerCustomerId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(CUSTOMER_ID_HEADER, callerCustomerId);
        try {
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Customer {} not found in User Service", callerCustomerId);
            throw new CustomerNotFoundException("Customer not found: " + callerCustomerId);
        } catch (HttpClientErrorException ex) {
            log.error("User Service rejected validation for customer {}: {}",
                    callerCustomerId, ex.getStatusCode(), ex);
            throw new UserServiceUnavailableException("Could not verify customer " + callerCustomerId, ex);
        } catch (RestClientException ex) {
            // Covers connection refused / Eureka lookup failure, e.g. User Service not deployed yet.
            log.error("User Service unreachable while validating customer {}", callerCustomerId, ex);
            throw new UserServiceUnavailableException("Could not verify customer " + callerCustomerId, ex);
        }
    }

    private Account loadOwnedAccount(String accountId, String callerCustomerId) {
        Account account = repository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        if (!account.getCustomerId().equals(callerCustomerId)) {
            log.warn("Customer {} attempted to act on account {} owned by {}",
                    callerCustomerId, accountId, account.getCustomerId());
            throw new UnauthorizedAccountAccessException("You do not own this account");
        }
        return account;
    }

    private BigDecimal maxBalanceFor(AccountType type) {
        return type == AccountType.SAVINGS ? savingsMaxBalance : currentMaxBalance;
    }

    private BigDecimal dailyTransferLimitFor(AccountType type) {
        return type == AccountType.SAVINGS ? savingsDailyTransferLimit : currentDailyTransferLimit;
    }

    private int dailyTransactionLimitFor(AccountType type) {
        return type == AccountType.SAVINGS ? savingsDailyTransactionLimit : currentDailyTransactionLimit;
    }

    private boolean isNotPositive(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) <= 0;
    }
    private void debit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
        account.setDailyTransferredAmount(account.getDailyTransferredAmount().add(amount));
        account.setTodayTransactionCount(account.getTodayTransactionCount() + 1);
        account.setUpdatedAt(LocalDateTime.now());
    }

    private void applyOutgoingLimitChecks(String callerCustomerId, TransactionType type,
                                           Account account, BigDecimal amount) {
        applyOutgoingLimitChecks(callerCustomerId, type, account, amount, account.getId());
    }

    private void applyOutgoingLimitChecks(String callerCustomerId, TransactionType type,
                                           Account account, BigDecimal amount, String receiverId) {
        if (account.getTodayTransactionCount() + 1 > account.getDailyTransactionLimit()) {
            log.warn("Daily transaction count limit reached for account {}", account.getId());
            transactionRecorder.recordFailure(callerCustomerId, type, account.getId(), receiverId,
                    amount, FailureReason.DAILY_LIMIT_EXCEEDED);
            throw new DailyLimitExceededException("Daily transaction count limit reached");
        }
        if (account.getDailyTransferredAmount().add(amount).compareTo(account.getDailyTransferLimit()) > 0) {
            log.warn("Daily transfer amount limit reached for account {}", account.getId());
            transactionRecorder.recordFailure(callerCustomerId, type, account.getId(), receiverId,
                    amount, FailureReason.DAILY_LIMIT_EXCEEDED);
            throw new DailyLimitExceededException("Daily transfer amount limit reached");
        }
    }
}
