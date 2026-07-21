package com.smartbank.wallet.exception;

import com.smartbank.wallet.constants.FailureReason;
import lombok.Getter;

// Thrown inside the service when a business rule is broken (not enough money, over a limit, etc.).
// We catch it, record a FAILED transaction, and return a 200 - it never leaves as an HTTP error.
@Getter
public class BusinessRuleException extends RuntimeException {

    private final FailureReason failureReason;

    public BusinessRuleException(FailureReason failureReason, String message) {
        super(message);
        this.failureReason = failureReason;
    }
}
