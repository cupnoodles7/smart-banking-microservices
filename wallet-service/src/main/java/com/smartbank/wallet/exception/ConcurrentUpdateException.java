package com.smartbank.wallet.exception;

// We kept losing the race to update a wallet and gave up after retrying. Comes back as a 409 Conflict.
public class ConcurrentUpdateException extends RuntimeException {
    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
