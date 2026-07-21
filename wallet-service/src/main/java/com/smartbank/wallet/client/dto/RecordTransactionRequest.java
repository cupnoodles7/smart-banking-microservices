package com.smartbank.wallet.client.dto;

import com.smartbank.wallet.constants.FailureReason;
import com.smartbank.wallet.constants.PartyType;
import com.smartbank.wallet.constants.TransactionStatus;
import com.smartbank.wallet.constants.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ledger write payload mirroring the Transaction schema (PRD §6.5).
 * Records are write-once and already in their final state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordTransactionRequest {

    private TransactionType transactionType;
    private PartyType senderType;
    private String senderId;
    private PartyType receiverType;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private FailureReason failureReason;
    private String idempotencyKey;
    private Instant initiatedAt;
    private Instant completedAt;
}
