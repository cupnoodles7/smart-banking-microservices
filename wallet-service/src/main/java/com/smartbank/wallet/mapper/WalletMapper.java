package com.smartbank.wallet.mapper;

import com.smartbank.wallet.dto.response.WalletResponse;
import com.smartbank.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

/**
 * Maps Wallet entities to response DTOs (PRD §6.10 — entities are never returned
 * from a controller).
 */
@Component
public class WalletMapper {

    public WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .customerId(wallet.getCustomerId())
                .linkedAccountId(wallet.getLinkedAccountId())
                .walletType(wallet.getWalletType())
                .balance(wallet.getBalance())
                .maxBalance(wallet.getMaxBalance())
                .dailyTransferLimit(wallet.getDailyTransferLimit())
                .dailyTransferredAmount(wallet.getDailyTransferredAmount())
                .dailyTransactionLimit(wallet.getDailyTransactionLimit())
                .todayTransactionCount(wallet.getTodayTransactionCount())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
