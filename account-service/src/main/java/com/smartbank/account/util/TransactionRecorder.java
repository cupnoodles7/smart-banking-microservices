package com.smartbank.account.util;

import com.smartbank.account.dto.request.RecordTransactionRequest;
import com.smartbank.account.entity.FailureReason;
import com.smartbank.account.entity.ReceiverType;
import com.smartbank.account.entity.SenderType;
import com.smartbank.account.entity.TransactionStatus;
import com.smartbank.account.entity.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Reports Deposit/Withdraw/Transfer outcomes - SUCCESS or FAILED - to Transaction
// Service's immutable ledger via POST /transactions/internal (PRD sec 6.7). Account
// Service owns the business decision; Transaction Service only records it, so this
// is a best-effort, fire-and-forget call: if it fails or Transaction Service is
// unreachable, the caller is NOT affected - it is only logged as an ERROR.
//
// Skippable locally via transaction-service.enabled: false.
@Component
public class TransactionRecorder {

    private static final Logger log = LoggerFactory.getLogger(TransactionRecorder.class);

    private final RestTemplate restTemplate;
    private final boolean enabled;
    private final String recordUrl;

    public TransactionRecorder(RestTemplate restTemplate,
                                @Value("${transaction-service.enabled:true}") boolean enabled,
                                @Value("${transaction-service.url}") String transactionServiceUrl) {
        this.restTemplate = restTemplate;
        this.enabled = enabled;
        this.recordUrl = transactionServiceUrl + "/transactions/internal";
    }

    // Deposit: cash enters the account from outside the ledger's account graph.
    // With no external party type available on Transaction Service's side, the
    // account is recorded as both sender and receiver of its own inbound cash.
    public void recordDeposit(String customerId, String accountId, BigDecimal amount, LocalDateTime completedAt) {
        record(customerId, TransactionType.DEPOSIT, accountId, accountId, amount,
                TransactionStatus.SUCCESS, FailureReason.NONE, completedAt);
    }

    // Withdraw: cash leaves the account to outside the ledger's account graph,
    // recorded the same way as a deposit but as an outgoing movement.
    public void recordWithdraw(String customerId, String accountId, BigDecimal amount, LocalDateTime completedAt) {
        record(customerId, TransactionType.WITHDRAW, accountId, accountId, amount,
                TransactionStatus.SUCCESS, FailureReason.NONE, completedAt);
    }

    // Transfer: money moves from one account to another.
    public void recordTransfer(String customerId, String fromAccountId, String toAccountId,
                                BigDecimal amount, LocalDateTime completedAt) {
        record(customerId, TransactionType.TRANSFER, fromAccountId, toAccountId, amount,
                TransactionStatus.SUCCESS, FailureReason.NONE, completedAt);
    }

    // A business rule blocked the operation. Still recorded for audit, with the
    // matching FailureReason, before the caller throws its exception.
    public void recordFailure(String customerId, TransactionType type, String senderId, String receiverId,
                               BigDecimal amount, FailureReason reason) {
        record(customerId, type, senderId, receiverId, amount,
                TransactionStatus.FAILED, reason, LocalDateTime.now());
    }

    private void record(String customerId, TransactionType type, String senderId, String receiverId,
                         BigDecimal amount, TransactionStatus status, FailureReason reason,
                         LocalDateTime completedAt) {
        if (!enabled) {
            log.warn("Skipping Transaction Service recording for {} on account {} - "
                    + "transaction-service.enabled=false", type, senderId);
            return;
        }

        RecordTransactionRequest request = RecordTransactionRequest.builder()
                .customerId(customerId)
                .transactionType(type)
                .senderType(SenderType.ACCOUNT)
                .senderId(senderId)
                .receiverType(ReceiverType.ACCOUNT)
                .receiverId(receiverId)
                .amount(amount)
                .status(status)
                .failureReason(reason)
                .idempotencyKey(UUID.randomUUID().toString())
                .initiatedAt(completedAt)
                .completedAt(completedAt)
                .build();

        try {
            restTemplate.postForEntity(recordUrl, request, Void.class);
        } catch (Exception ex) {
            // Fire-and-forget: covers RestClientException (connection refused, timeout,
            // non-2xx response) AND IllegalStateException, which is what Spring Cloud
            // LoadBalancer throws - not a RestClientException - when no instance of
            // "transaction-service" is currently registered in Eureka. Either way,
            // Transaction Service being unavailable must never fail the caller's
            // deposit/withdraw/transfer (PRD sec 6.7).
            log.error("Transaction Service unavailable while recording {} for account {}",
                    type, senderId, ex);
        }
    }
}