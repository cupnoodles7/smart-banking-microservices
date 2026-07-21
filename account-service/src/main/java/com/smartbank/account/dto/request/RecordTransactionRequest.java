package com.smartbank.account.dto.request;

import com.smartbank.account.entity.FailureReason;
import com.smartbank.account.entity.ReceiverType;
import com.smartbank.account.entity.SenderType;
import com.smartbank.account.entity.TransactionStatus;
import com.smartbank.account.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Outbound body for Transaction Service's POST /transactions/internal (PRD sec 6.7).
// Field-for-field mirror of Transaction Service's own RecordTransactionRequest, kept
// as a local copy since Account Service must never depend on another service's code.
@Data
@Builder
public class RecordTransactionRequest {

    private String customerId;

    private TransactionType transactionType;

    private SenderType senderType;
    private String senderId;

    private ReceiverType receiverType;
    private String receiverId;

    private BigDecimal amount;

    private TransactionStatus status;

    private FailureReason failureReason;

    private String idempotencyKey;

    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
}
