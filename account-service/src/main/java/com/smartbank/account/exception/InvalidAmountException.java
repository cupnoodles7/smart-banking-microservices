package com.smartbank.account.exception;

// Amount is null, <= 0, or otherwise malformed. Mapped to 400 (PRD sec 6.14).
// Also used when a deposit would push balance above the account type's maxBalance,
// since the PRD's FailureReason enum has no dedicated "max balance" value.
public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
