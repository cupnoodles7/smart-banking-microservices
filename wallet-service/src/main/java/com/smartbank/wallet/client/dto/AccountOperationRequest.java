package com.smartbank.wallet.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// What we send the Account Service to deposit or withdraw money. The idempotencyKey keeps retries safe.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOperationRequest {
    private String accountId;
    private BigDecimal amount;
    private String idempotencyKey;
}
