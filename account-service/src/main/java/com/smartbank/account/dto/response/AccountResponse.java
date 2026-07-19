package com.smartbank.account.dto.response;

import com.smartbank.account.entity.Account;
import com.smartbank.account.entity.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Read model returned to callers so the MongoDB entity is never exposed directly
// (PRD sec 6.13 DTO strategy). Entity -> DTO mapping lives here as a static
// factory rather than a separate mapper class.
@Data
@Builder
public class AccountResponse {

    private String id;
    private String customerId;
    private AccountType accountType;

    private BigDecimal balance;
    private BigDecimal maxBalance;

    private BigDecimal dailyTransferLimit;
    private BigDecimal dailyTransferredAmount;

    private int dailyTransactionLimit;
    private int todayTransactionCount;

    private String currency;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .maxBalance(account.getMaxBalance())
                .dailyTransferLimit(account.getDailyTransferLimit())
                .dailyTransferredAmount(account.getDailyTransferredAmount())
                .dailyTransactionLimit(account.getDailyTransactionLimit())
                .todayTransactionCount(account.getTodayTransactionCount())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
