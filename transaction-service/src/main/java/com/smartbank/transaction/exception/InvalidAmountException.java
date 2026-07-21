package com.smartbank.transaction.exception;

// Amount is null or not greater than zero -> HTTP 400 
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
