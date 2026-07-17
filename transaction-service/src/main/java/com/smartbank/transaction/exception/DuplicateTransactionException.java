package com.smartbank.transaction.exception;

// A conflicting write hit the unique idempotencyKey index and no prior record could be
// re-read -> HTTP 409. The normal idempotent retry path returns the stored record instead
// of throwing this (PRD sec 6.15).
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }
}
