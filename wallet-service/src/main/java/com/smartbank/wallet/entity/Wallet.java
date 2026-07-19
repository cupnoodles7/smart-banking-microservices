package com.smartbank.wallet.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Stored-value wallet document — {@code wallet_db.wallets} (PRD §6.5).
 *
 * <p>Holds real money and is linked one-to-one to an Account
 * ({@code linkedAccountId} is unique). Daily counters are reset lazily
 * (PRD §6.14); {@code version} drives optimistic locking on every
 * balance-mutating write (PRD §6.15).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallets")
public class Wallet {

    @Id
    private String id;

    @Indexed
    private String customerId;

    /** Exactly one wallet per linked account (PRD §6.6). */
    @Indexed(unique = true)
    private String linkedAccountId;

    private WalletType walletType;

    private BigDecimal balance;

    /** Hard cap on balance — ₹50,000 (PRD §6.6). */
    private BigDecimal maxBalance;

    /** Max value that may leave the wallet per day — ₹20,000 (PRD §6.6). */
    private BigDecimal dailyTransferLimit;

    private BigDecimal dailyTransferredAmount;

    /** Max wallet transactions per day — 20 (PRD §6.6). */
    private int dailyTransactionLimit;

    private int todayTransactionCount;

    /** Date the daily counters were last reset (PRD §6.14). */
    private LocalDate lastLimitResetDate;

    /** Optimistic-lock counter (PRD §6.15). */
    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
