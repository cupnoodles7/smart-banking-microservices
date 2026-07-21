package com.smartbank.user.exception;

/** Raised when an email is missing '@' (PRD sec 7.2 -> 400). */
public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String message) {
        super(message);
    }
}
