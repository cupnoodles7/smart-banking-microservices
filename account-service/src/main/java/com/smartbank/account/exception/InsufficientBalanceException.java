package com.smartbank.account.exception;

// Withdraw/transfer amount exceeds the current balance. Mapped to 400 (PRD sec 6.14).
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
