package com.smartbank.user.exception;

// Raised when a phone number is not exactly 10 digits 
public class InvalidPhoneNumberException extends RuntimeException {

    public InvalidPhoneNumberException(String message) {
        super(message);
    }
}
