package com.smartbank.transaction.exception;

// Sender and receiver are the same on a transfer operation -> HTTP 400 (PRD sec 7.3).
public class SelfTransferException extends RuntimeException {

    public SelfTransferException(String message) {
        super(message);
    }
}
