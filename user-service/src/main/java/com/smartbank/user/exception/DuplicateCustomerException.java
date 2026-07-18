package com.smartbank.user.exception;

/** Raised when an email or phone is already in use (PRD sec 7.2 -> 409). */
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String message) {
        super(message);
    }
}
