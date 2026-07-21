package com.smartbank.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// What the caller sends to top up a wallet from its linked bank account.
@Data
public class TopupRequest {

    @NotBlank(message = "walletId is required")
    private String walletId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
}
