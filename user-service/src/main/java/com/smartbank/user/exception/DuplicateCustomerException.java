package com.smartbank.user.exception;

// Raised when an email or phone is already in use 
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String message) {
        super(message);
    }
}
