package com.smartbank.transaction.exception;

// Amount is null or not greater than zero -> HTTP 400 (PRD sec 6.7 validation).
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
