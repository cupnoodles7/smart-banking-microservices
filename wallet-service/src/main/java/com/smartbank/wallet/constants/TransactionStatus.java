package com.smartbank.wallet.constants;

/**
 * Final outcome of a transaction (PRD §6.5). Records are written exactly once,
 * already in their final state — there is no INITIATED status (PRD §1.1 item 1).
 */
public enum TransactionStatus {
    SUCCESS,
    FAILED
}
