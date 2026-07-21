package com.smartbank.transaction.dto.request;

import com.smartbank.transaction.entity.FailureReason;
import com.smartbank.transaction.entity.ReceiverType;
import com.smartbank.transaction.entity.SenderType;
import com.smartbank.transaction.entity.TransactionStatus;
import com.smartbank.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Body for POST /transactions/internal
@Data
public class RecordTransactionRequest {

    @NotBlank
    private String customerId;

    @NotNull
    private TransactionType transactionType;

    @NotNull
    private SenderType senderType;
    @NotBlank
    private String senderId;

    @NotNull
    private ReceiverType receiverType;
    @NotBlank
    private String receiverId;

    // >0 is enforced in the service so it can log WARN and raise InvalidAmountException.
    @NotNull
    private BigDecimal amount;

    @NotNull
    private TransactionStatus status; 

    private FailureReason failureReason; 

    @NotBlank
    private String idempotencyKey; 

    private LocalDateTime initiatedAt; 
    private LocalDateTime completedAt; 
}
