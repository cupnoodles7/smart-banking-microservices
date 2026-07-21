package com.smartbank.account.exception;

// Thrown when a lookup by account id finds nothing. Mapped to 404 (PRD sec 6.14).
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
