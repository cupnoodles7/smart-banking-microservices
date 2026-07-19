package com.smartbank.wallet.constants;

/**
 * Why a transaction ended up FAILED (PRD §6.5). {@code NONE} is used for SUCCESS.
 *
 * <p>Per PRD §7.3, business-rule failures never throw across the REST boundary —
 * they are returned as HTTP 200 with a FAILED transaction carrying one of these
 * reasons.
 */
public enum FailureReason {
    NONE,
    INVALID_AMOUNT,
    INVALID_ACCOUNT,
    INVALID_WALLET,
    INSUFFICIENT_BALANCE,
    DAILY_LIMIT_EXCEEDED,
    WALLET_LIMIT_EXCEEDED,
    SELF_TRANSFER
}
