package com.smartbank.transaction.exception;

// A required field is missing or the request is otherwise malformed -> HTTP 400.
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}
