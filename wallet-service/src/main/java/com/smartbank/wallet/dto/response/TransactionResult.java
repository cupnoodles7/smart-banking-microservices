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

/**
 * Result of a money-moving wallet operation (top-up / transfer / bill payment).
 *
 * <p>Per PRD §7.3 a business-rule violation is NOT an HTTP error — it comes back
 * as HTTP 200 with {@code status = FAILED} and a {@link FailureReason}. Only
 * structural problems (bad auth, malformed request, missing wallet) produce the
 * §6.9 error shape.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult {

    private TransactionStatus status;
    private FailureReason failureReason;
    private TransactionType transactionType;
    private BigDecimal amount;

    /** The wallet from which the operation was initiated. */
    private String walletId;

    /** Resulting balance of {@code walletId} (null when the op did not run). */
    private BigDecimal resultingBalance;

    private String idempotencyKey;
    private String message;
    private Instant timestamp;
}
