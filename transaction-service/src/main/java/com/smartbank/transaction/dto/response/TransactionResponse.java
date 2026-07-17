package com.smartbank.transaction.dto.response;

import com.smartbank.transaction.entity.FailureReason;
import com.smartbank.transaction.entity.ReceiverType;
import com.smartbank.transaction.entity.SenderType;
import com.smartbank.transaction.entity.TransactionStatus;
import com.smartbank.transaction.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Read model returned to callers so the MongoDB entity is never exposed directly (PRD sec 6.7).
@Data
@Builder
public class TransactionResponse {

    private String id;
    private String customerId;

    private TransactionType transactionType;

    private SenderType senderType;
    private String senderId;

    private ReceiverType receiverType;
    private String receiverId;

    private BigDecimal amount;
    private String currency;

    private TransactionStatus status;
    private FailureReason failureReason;

    private String idempotencyKey;

    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
}
