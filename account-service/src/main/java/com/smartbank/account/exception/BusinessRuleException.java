package com.smartbank.account.exception;

// A business rule was violated by an otherwise well-formed request (maps to HTTP 400).
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
