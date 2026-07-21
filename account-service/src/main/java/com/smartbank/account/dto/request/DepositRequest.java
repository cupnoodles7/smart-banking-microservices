package com.smartbank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class DepositRequest {

    @NotBlank
    private String accountId;

    @NotNull
    private BigDecimal amount;
}

