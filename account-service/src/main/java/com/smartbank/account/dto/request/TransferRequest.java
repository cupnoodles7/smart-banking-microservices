package com.smartbank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// Body for POST /accounts/transfer (PRD sec 6.10). Account-to-account transfer only;
// wallet-involving transfers are Wallet Service's responsibility (PRD sec 6.4).
@Data
public class TransferRequest {

    @NotBlank
    private String fromAccountId;

    @NotBlank
    private String toAccountId;

    @NotNull
    private BigDecimal amount;
}

