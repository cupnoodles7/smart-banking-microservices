package com.smartbank.account.entity;

// Party type on the sending side of a recorded transaction (PRD sec 6.7). Account
// Service only ever moves money out of accounts, so this is always ACCOUNT; the
// value exists (rather than a hardcoded string) because Transaction Service's
// RecordTransactionRequest requires it as a typed field.
public enum SenderType {
    ACCOUNT
}
