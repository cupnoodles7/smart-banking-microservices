package com.smartbank.account.entity;

// Why a FAILED transaction failed; NONE for successful ones (PRD sec 6.5). Mirrors
// only the reasons Account Service itself can produce.
public enum FailureReason {
    NONE,
    INVALID_AMOUNT,
    INVALID_ACCOUNT,
    INSUFFICIENT_BALANCE,
    DAILY_LIMIT_EXCEEDED,
    SELF_TRANSFER
}
