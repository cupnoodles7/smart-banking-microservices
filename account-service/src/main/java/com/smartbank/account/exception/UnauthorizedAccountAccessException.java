package com.smartbank.account.exception;

// Raised when the caller's identity is missing (no X-Customer-Id header, set by the
// Gateway after JWT validation) or when it does not match the owner of the
// account/customer being acted on. Mapped to 403 (PRD sec 6.14) - mirrors User
// Service's ForbiddenException, which handles the same two cases for /users/{id}.
public class UnauthorizedAccountAccessException extends RuntimeException {
    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
