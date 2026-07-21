package com.smartbank.account.exception;

// Sender and receiver account ids are the same on a transfer (PRD sec 7.3).
// Mapped to 400 (PRD sec 6.14).
public class SelfTransferException extends RuntimeException {
    public SelfTransferException(String message) {
        super(message);
    }
}
