package com.smartbank.wallet.dto.response;

import com.smartbank.wallet.entity.WalletType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

// The wallet details we show to clients - we never hand back the raw database entity.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private String id;
    private String customerId;
    private String linkedAccountId;
    private WalletType walletType;
    private BigDecimal balance;
    private BigDecimal maxBalance;
    private BigDecimal dailyTransferLimit;
    private BigDecimal dailyTransferredAmount;
    private int dailyTransactionLimit;
    private int todayTransactionCount;
    private Instant createdAt;
    private Instant updatedAt;
}
