package com.smartbank.user.exception;

// Raised when an email is missing '@'
public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String message) {
        super(message);
    }
}
