package com.smartbank.wallet.exception;

/**
 * Optimistic-lock retries were exhausted on a balance-mutating write (PRD §6.15).
 * Structural problem → HTTP 409 Conflict (PRD §10 scenario 8).
 */
public class ConcurrentUpdateException extends RuntimeException {
    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
