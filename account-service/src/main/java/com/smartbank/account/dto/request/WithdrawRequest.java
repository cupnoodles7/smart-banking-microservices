package com.smartbank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// Body for POST /accounts/withdraw (PRD sec 6.10).
@Data
public class WithdrawRequest {

    @NotBlank
    private String accountId;

    @NotNull
    private BigDecimal amount;
}
