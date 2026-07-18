package com.smartbank.user.exception;

/** Raised when a phone number is not exactly 10 digits (PRD sec 7.2 -> 400). */
public class InvalidPhoneNumberException extends RuntimeException {

    public InvalidPhoneNumberException(String message) {
        super(message);
    }
}
