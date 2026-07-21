package com.smartbank.user.exception;

// Raised when no profile exists for the requested id 
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
