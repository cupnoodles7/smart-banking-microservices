package com.smartbank.wallet.exception;

/**
 * The linked account already has a wallet — one wallet per account (PRD §6.6).
 * Structural problem → HTTP 409 (PRD §7.3).
 */
public class DuplicateWalletException extends RuntimeException {
    public DuplicateWalletException(String message) {
        super(message);
    }
}
