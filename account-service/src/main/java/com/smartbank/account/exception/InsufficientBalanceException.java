package com.smartbank.account.exception;

// Withdraw/transfer amount exceeds the current balance.
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
