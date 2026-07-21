package com.smartbank.user.exception;

/**
 * Raised when the authenticated caller tries to act on a profile that is not their own,
 * or presents an invalid internal API key. Structural auth failure -> 403 (PRD sec 7.3).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
