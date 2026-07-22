package com.smartbank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {

    @NotBlank
    private String accountId;

    @NotNull
    private BigDecimal amount;

    // Optional. When supplied (e.g. by the Wallet Service on top-up debit), the same key is applied
    // at most once so a retry after a timeout cannot debit the account twice. Direct API callers may
    // omit it, in which case the operation is not deduplicated.
    private String idempotencyKey;
}
