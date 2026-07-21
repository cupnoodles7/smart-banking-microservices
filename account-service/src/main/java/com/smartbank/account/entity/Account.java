package com.smartbank.account.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// One Savings or Current account (PRD sec 6.5.1). Balance is stored directly, not
// derived from transaction history. Daily counters are reset lazily on first use
// of a new day (PRD sec 6.9), never via a scheduled job.
@Document(collection = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private String id; // Mongo document id

    @Indexed
    private String customerId; // owner of the account; drives GET /accounts/customer/{id}

    private AccountType accountType; // SAVINGS or CURRENT

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO; // stored directly, never calculated

    private BigDecimal maxBalance;          // account-type limit at creation time (PRD sec 6.5.1)
    private BigDecimal dailyTransferLimit;  // account-type limit at creation time
    private int dailyTransactionLimit;      // account-type limit at creation time

    @Builder.Default
    private BigDecimal dailyTransferredAmount = BigDecimal.ZERO; // reset lazily, see util.DailyLimitResetEvaluator

    @Builder.Default
    private int todayTransactionCount = 0; // reset lazily

    private LocalDate lastLimitResetDate; // last day the counters above were valid for

    @Builder.Default
    private String currency = "INR"; // PRD sec 6.5 default currency

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
