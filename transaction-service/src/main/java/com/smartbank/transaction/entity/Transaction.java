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

@Document(collection = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    private String id;

    @Indexed
    private String customerId; 

    private TransactionType transactionType; 

    private SenderType senderType; 
    @Indexed
    private String senderId;       

    private ReceiverType receiverType; 
    @Indexed
    private String receiverId;         

    private BigDecimal amount;      

    @Builder.Default
    private String currency = TransactionConstants.DEFAULT_CURRENCY; 

    private TransactionStatus status;     
    private FailureReason failureReason;   

    @Indexed(unique = true, sparse = true)
    private String idempotencyKey; 

    private LocalDateTime initiatedAt; 
    private LocalDateTime completedAt; 
}
