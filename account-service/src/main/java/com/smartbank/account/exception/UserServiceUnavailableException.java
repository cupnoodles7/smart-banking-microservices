package com.smartbank.account.exception;

// User Service could not be reached to validate the customerId (down, not yet
// deployed, or a network/discovery failure). Mapped to 503 (PRD sec 6.14) - this
// is Account Service's own health being fine but a dependency being unavailable,
// distinct from a 500 (Account Service's own unexpected failure).
public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
