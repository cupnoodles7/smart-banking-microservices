package com.smartbank.transaction.entity;

import com.smartbank.transaction.constants.TransactionConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One immutable ledger record; written exactly once in its final state (PRD sec 6.5).
// The service never updates or deletes a stored document - it is a pure ledger.
@Document(collection = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    private String id; // Mongo document id

    @Indexed
    private String customerId; // owner of the transaction; drives GET /transactions/customer/{id}

    private TransactionType transactionType; // DEPOSIT, WITHDRAW, TRANSFER, ...

    private SenderType senderType; // ACCOUNT or WALLET
    @Indexed
    private String senderId;       // account/wallet id that sent the money

    private ReceiverType receiverType; // ACCOUNT, WALLET or MERCHANT
    @Indexed
    private String receiverId;         // account/wallet/merchant id that received it

    private BigDecimal amount;      // attempted/settled amount

    @Builder.Default
    private String currency = TransactionConstants.DEFAULT_CURRENCY; // defaults to INR (PRD sec 6.5)

    private TransactionStatus status;     // SUCCESS or FAILED
    private FailureReason failureReason;   // NONE when SUCCESS

    @Indexed(unique = true, sparse = true)
    private String idempotencyKey; // dedup key; unique per record (PRD sec 6.15)

    private LocalDateTime initiatedAt; // when the owning service began the operation
    private LocalDateTime completedAt; // when the final outcome was known
}
