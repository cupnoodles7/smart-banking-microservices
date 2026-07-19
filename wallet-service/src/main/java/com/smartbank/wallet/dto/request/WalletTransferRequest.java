package com.smartbank.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Wallet-to-wallet transfer (PRD §6.7 POST /wallets/transfer).
 *
 * <p>Same source and destination is a business-rule failure (SELF_TRANSFER →
 * HTTP 200 FAILED), not a 400 (PRD §7.3, §10 scenario 4b).
 */
@Data
public class WalletTransferRequest {

    @NotBlank(message = "fromWalletId is required")
    private String fromWalletId;

    @NotBlank(message = "toWalletId is required")
    private String toWalletId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
}
