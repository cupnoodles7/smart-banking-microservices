package com.smartbank.wallet.exception;

/**
 * A referenced wallet does not exist. Structural problem → HTTP 404 (PRD §7.3).
 */
public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String message) {
        super(message);
    }
}
