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

    public void recordDeposit(String customerId, String accountId, BigDecimal amount, LocalDateTime completedAt) {
        record(customerId, TransactionType.DEPOSIT, accountId, accountId, amount,
                TransactionStatus.SUCCESS, FailureReason.NONE, completedAt);
    }

    public void recordWithdraw(String customerId, String accountId, BigDecimal amount, LocalDateTime completedAt) {
        record(customerId, TransactionType.WITHDRAW, accountId, accountId, amount,
                TransactionStatus.SUCCESS, FailureReason.NONE, completedAt);
    }

    public void recordTransfer(String customerId, String fromAccountId, String toAccountId,
                                BigDecimal amount, LocalDateTime completedAt) {
        record(customerId, TransactionType.TRANSFER, fromAccountId, toAccountId, amount,
                TransactionStatus.SUCCESS, FailureReason.NONE, completedAt);
    }

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
            log.error("Transaction Service unavailable while recording {} for account {}",
                    type, senderId, ex);
        }
    }
}