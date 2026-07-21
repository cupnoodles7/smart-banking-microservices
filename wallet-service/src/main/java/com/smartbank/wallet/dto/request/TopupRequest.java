package com.smartbank.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Top up a wallet from its linked account (PRD §6.7 POST /wallets/topup).
 *
 * <p>{@code amount} is only {@code @NotNull}, not {@code @Positive}: a non-positive
 * amount is a business-rule failure (INVALID_AMOUNT → HTTP 200 FAILED), not a
 * malformed request (PRD §7.3, §10 scenario 1).
 */
@Data
public class TopupRequest {

    @NotBlank(message = "walletId is required")
    private String walletId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
}
