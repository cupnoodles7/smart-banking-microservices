package com.smartbank.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Pay a bill from a wallet (PRD §6.7 POST /wallets/pay-bill).
 * The receiver is a MERCHANT ({@code billerId}); there is no wallet to credit.
 */
@Data
public class PayBillRequest {

    @NotBlank(message = "walletId is required")
    private String walletId;

    @NotBlank(message = "billerId is required")
    private String billerId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
}
