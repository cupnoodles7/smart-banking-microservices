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

// A customer's wallet as we store it - real money, tied one-to-one to a bank account.
// The version field lets us catch two updates racing on the same wallet.
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

    // Only one wallet is allowed per bank account.
    @Indexed(unique = true)
    private String linkedAccountId;

    private WalletType walletType;

    private BigDecimal balance;

    // The most this wallet can ever hold.
    private BigDecimal maxBalance;

    // The most that can leave this wallet in a single day.
    private BigDecimal dailyTransferLimit;

    private BigDecimal dailyTransferredAmount;

    // How many transactions the wallet is allowed in a day.
    private int dailyTransactionLimit;

    private int todayTransactionCount;

    // The day we last reset the daily counters above.
    private LocalDate lastLimitResetDate;

    // Bumped on every save so we can spot two updates clashing on the same wallet.
    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
