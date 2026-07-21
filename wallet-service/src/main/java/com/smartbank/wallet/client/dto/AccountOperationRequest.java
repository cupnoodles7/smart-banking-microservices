package com.smartbank.wallet.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body for Account Service deposit/withdraw (PRD §6.7). Carries an
 * idempotencyKey so the debit and its reversal are each safely retryable (§6.15).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOperationRequest {
    private String accountId;
    private BigDecimal amount;
    private String idempotencyKey;
}
