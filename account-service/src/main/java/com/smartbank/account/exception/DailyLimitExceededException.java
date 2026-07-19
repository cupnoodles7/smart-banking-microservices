package com.smartbank.account.exception;

// Either the daily transfer amount cap or the daily transaction count cap has been
// hit for this account (PRD sec 6.5.1). Mapped to 400 (PRD sec 6.14).
public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException(String message) {
        super(message);
    }
}

