package com.smartbank.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// The error body we send when something is genuinely wrong (bad auth, missing wallet, a conflict).
// Business-rule failures don't use this - they come back as a FAILED TransactionResult.
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
