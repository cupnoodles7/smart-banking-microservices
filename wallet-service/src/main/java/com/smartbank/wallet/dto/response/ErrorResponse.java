package com.smartbank.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error shape for structural failures (PRD §6.9): bad auth, malformed
 * requests, not-found resources, optimistic-lock conflicts. Business-rule failures
 * do NOT use this shape (see {@link TransactionResult}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
