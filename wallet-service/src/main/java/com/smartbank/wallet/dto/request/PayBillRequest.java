package com.smartbank.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// What the caller sends to pay a bill from a wallet. The money goes to a merchant, so there's no wallet to credit.
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
