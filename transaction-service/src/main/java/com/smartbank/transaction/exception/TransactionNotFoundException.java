package com.smartbank.transaction.exception;

// No transaction exists for the requested id / idempotency key -> HTTP 404 
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }
}
