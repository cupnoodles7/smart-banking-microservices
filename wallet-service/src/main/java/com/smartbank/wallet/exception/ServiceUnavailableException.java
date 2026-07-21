package com.smartbank.wallet.exception;

// A downstream service (Account or Transaction) is temporarily unavailable.
// Raised by circuit-breaker fallbacks so the caller gets a clear 503 instead of a raw transport error.
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
