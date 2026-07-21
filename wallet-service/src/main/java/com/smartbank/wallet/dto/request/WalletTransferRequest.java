package com.smartbank.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// What the caller sends to move money from one wallet to another.
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
