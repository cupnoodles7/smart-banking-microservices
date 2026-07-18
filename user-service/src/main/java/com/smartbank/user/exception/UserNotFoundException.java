package com.smartbank.user.exception;

/** Raised when no profile exists for the requested id (PRD sec 6.7 -> 404). */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
