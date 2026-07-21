package com.smartbank.account.exception;

// The customerId (from X-Customer-Id) does not correspond to a real, registered
// customer in User Service. Mapped to 404 (PRD sec 6.14).
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
