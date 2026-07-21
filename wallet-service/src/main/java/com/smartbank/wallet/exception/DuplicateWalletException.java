package com.smartbank.wallet.exception;

// That bank account already has a wallet - you only get one per account (409 Conflict).
public class DuplicateWalletException extends RuntimeException {
    public DuplicateWalletException(String message) {
        super(message);
    }
}
