package com.smartbank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// Body for POST /accounts/deposit (PRD sec 6.10).
@Data
public class DepositRequest {

    @NotBlank
    private String accountId;

    // >0 and max-balance checks happen in the service layer so failures can be
    // logged and recorded to the ledger with the right FailureReason.
    @NotNull
    private BigDecimal amount;
}

