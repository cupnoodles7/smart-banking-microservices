package com.smartbank.wallet.dto.response;

import com.smartbank.wallet.constants.FailureReason;
import com.smartbank.wallet.constants.TransactionStatus;
import com.smartbank.wallet.constants.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

// How a wallet operation (top-up, transfer, bill payment) turned out. A broken business
// rule isn't an error here - it comes back as a FAILED result with a 200.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult {

    private TransactionStatus status;
    private FailureReason failureReason;
    private TransactionType transactionType;
    private BigDecimal amount;

    // The wallet the operation started from.
    private String walletId;

    // The wallet's balance afterwards (null if the operation never ran).
    private BigDecimal resultingBalance;

    private String idempotencyKey;
    private String message;
    private Instant timestamp;
}
