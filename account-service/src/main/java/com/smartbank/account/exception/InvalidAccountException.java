package com.smartbank.account.exception;

// Account id referenced by a transfer's counterparty does not exist, or the account
// type/customer combination is otherwise invalid. Mapped to 400 (PRD sec 6.14).
public class InvalidAccountException extends RuntimeException {
    public InvalidAccountException(String message) {
        super(message);
    }
}
