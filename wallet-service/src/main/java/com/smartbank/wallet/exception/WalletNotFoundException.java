package com.smartbank.wallet.exception;

// The wallet you asked for doesn't exist (404 Not Found).
public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String message) {
        super(message);
    }
}
