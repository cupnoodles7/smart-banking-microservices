package com.smartbank.transaction.exception;

// Thrown when the internal ledger-write endpoint is hit without a valid X-Internal-Api-Key. Maps to 401.
public class UnauthorizedInternalAccessException extends RuntimeException {

    public UnauthorizedInternalAccessException(String message) {
        super(message);
    }
}
