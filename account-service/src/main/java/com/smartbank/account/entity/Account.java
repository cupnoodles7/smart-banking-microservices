package com.smartbank.account.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private String id;

    @Indexed
    private String customerId;

    private AccountType accountType; 

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    private BigDecimal maxBalance;          
    private BigDecimal dailyTransferLimit; 
    private int dailyTransactionLimit;      

    @Builder.Default
    private BigDecimal dailyTransferredAmount = BigDecimal.ZERO; 

    @Builder.Default
    private int todayTransactionCount = 0; 

    private LocalDate lastLimitResetDate;

    @Builder.Default
    private String currency = "INR"; 

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version
    private Long version;
}
