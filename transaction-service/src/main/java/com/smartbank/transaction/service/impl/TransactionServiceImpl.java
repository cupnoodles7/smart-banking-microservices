package com.smartbank.transaction.service.impl;

import com.smartbank.transaction.dto.request.RecordTransactionRequest;
import com.smartbank.transaction.dto.response.TransactionResponse;
import com.smartbank.transaction.entity.FailureReason;
import com.smartbank.transaction.entity.Transaction;
import com.smartbank.transaction.entity.TransactionType;
import com.smartbank.transaction.exception.DuplicateTransactionException;
import com.smartbank.transaction.exception.InvalidAmountException;
import com.smartbank.transaction.exception.InvalidTransactionException;
import com.smartbank.transaction.exception.SelfTransferException;
import com.smartbank.transaction.exception.TransactionNotFoundException;
import com.smartbank.transaction.repository.TransactionRepository;
import com.smartbank.transaction.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    // Money-movement types where sender and receiver must differ (PRD sec 7.3).
    private static final Set<TransactionType> TRANSFER_TYPES =
            EnumSet.of(TransactionType.TRANSFER, TransactionType.WALLET_TRANSFER);

    private final TransactionRepository repository;

    public TransactionServiceImpl(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public TransactionResponse record(RecordTransactionRequest request) {
        validate(request);

        // Idempotent retry: return the record already stored under this key, never a duplicate.
        var existing = repository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.warn("Repeated idempotency key {} - returning existing transaction {}",
                    request.getIdempotencyKey(), existing.get().getId());
            return toResponse(existing.get());
        }

        Transaction toSave = Transaction.builder()
                .customerId(request.getCustomerId())
                .transactionType(request.getTransactionType())
                .senderType(request.getSenderType())
                .senderId(request.getSenderId())
                .receiverType(request.getReceiverType())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .status(request.getStatus())
                .failureReason(request.getFailureReason() == null
                        ? FailureReason.NONE : request.getFailureReason())
                .idempotencyKey(request.getIdempotencyKey())
                .initiatedAt(request.getInitiatedAt() == null
                        ? LocalDateTime.now() : request.getInitiatedAt())
                .completedAt(request.getCompletedAt() == null
                        ? LocalDateTime.now() : request.getCompletedAt())
                .build(); // currency defaults to INR via the entity builder

        Transaction saved;
        try {
            saved = repository.save(toSave);
        } catch (DuplicateKeyException ex) {
            // A concurrent write won the race on the unique idempotencyKey index.
            // Re-read and return that record so the caller still sees an idempotent result.
            var raced = repository.findByIdempotencyKey(request.getIdempotencyKey());
            if (raced.isPresent()) {
                log.warn("Concurrent write for idempotency key {} - returning stored transaction {}",
                        request.getIdempotencyKey(), raced.get().getId());
                return toResponse(raced.get());
            }
            throw new DuplicateTransactionException(
                    "Duplicate transaction for idempotency key " + request.getIdempotencyKey());
        }

        switch (saved.getStatus()) {
            case SUCCESS -> log.info("Recorded SUCCESS transaction {} ({}) amount {} {}",
                    saved.getId(), saved.getTransactionType(), saved.getAmount(), saved.getCurrency());
            case FAILED -> log.info("Recorded FAILED transaction {} ({}) reason {}",
                    saved.getId(), saved.getTransactionType(), saved.getFailureReason());
        }
        return toResponse(saved);
    }

    @Override
    public TransactionResponse getById(String id) {
        Transaction transaction = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id));
        log.info("Retrieved transaction {}", id);
        return toResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getByAccountId(String accountId, Pageable pageable) {
        log.info("Retrieving transactions for account {}", accountId);
        return repository.findBySenderIdOrReceiverId(accountId, accountId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<TransactionResponse> getByWalletId(String walletId, Pageable pageable) {
        log.info("Retrieving transactions for wallet {}", walletId);
        return repository.findBySenderIdOrReceiverId(walletId, walletId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<TransactionResponse> getByCustomerId(String customerId, Pageable pageable) {
        log.info("Retrieving transactions for customer {}", customerId);
        return repository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Override
    public TransactionResponse getByIdempotencyKey(String idempotencyKey) {
        Transaction transaction = repository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found for idempotency key: " + idempotencyKey));
        log.info("Retrieved transaction {} by idempotency key", transaction.getId());
        return toResponse(transaction);
    }

    // ---- Business validation (kept in the service layer, not only on the DTO) ----
    private void validate(RecordTransactionRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid amount {} for idempotency key {}", amount, request.getIdempotencyKey());
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (!StringUtils.hasText(request.getIdempotencyKey())) {
            throw new InvalidTransactionException("idempotencyKey must not be blank");
        }
        if (!StringUtils.hasText(request.getSenderId())) {
            throw new InvalidTransactionException("senderId is required");
        }
        if (!StringUtils.hasText(request.getReceiverId())) {
            throw new InvalidTransactionException("receiverId is required");
        }
        if (TRANSFER_TYPES.contains(request.getTransactionType())
                && request.getSenderId().equals(request.getReceiverId())) {
            log.warn("Self-transfer blocked: sender and receiver are both {}", request.getSenderId());
            throw new SelfTransferException("Sender and receiver must differ for a transfer");
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .customerId(t.getCustomerId())
                .transactionType(t.getTransactionType())
                .senderType(t.getSenderType())
                .senderId(t.getSenderId())
                .receiverType(t.getReceiverType())
                .receiverId(t.getReceiverId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .failureReason(t.getFailureReason())
                .idempotencyKey(t.getIdempotencyKey())
                .initiatedAt(t.getInitiatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
