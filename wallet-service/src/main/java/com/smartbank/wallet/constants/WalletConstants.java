package com.smartbank.wallet.constants;

import java.math.BigDecimal;

/**
 * Wallet business limits and shared constants (PRD §6.5, §6.6).
 * All money values are {@link BigDecimal} to avoid floating-point drift.
 */
public final class WalletConstants {

    private WalletConstants() {
    }

    /** Wallet balance may never exceed ₹50,000 (PRD §6.6). */
    public static final BigDecimal MAX_BALANCE = new BigDecimal("50000");

    /** At most ₹20,000 may leave a wallet per day (PRD §6.6). */
    public static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("20000");

    /** At most 20 wallet transactions per day (PRD §6.6). */
    public static final int DAILY_TRANSACTION_LIMIT = 20;

    /** Ledger currency (PRD §6.5). */
    public static final String CURRENCY = "INR";

    /** Optimistic-lock retry budget before returning 409 Conflict (PRD §6.15). */
    public static final int MAX_WRITE_RETRIES = 3;

    /** Compensating-reversal retry budget on partial failure (PRD §6.16). */
    public static final int MAX_REVERSAL_RETRIES = 3;

    /** Trusted identity header injected by the API Gateway after JWT validation. */
    public static final String HEADER_CUSTOMER_ID = "X-Customer-Id";

    /** Trusted identity header injected by the API Gateway after JWT validation. */
    public static final String HEADER_USER_EMAIL = "X-User-Email";
}
