package com.smartbank.account.entity;

public enum FailureReason {
    NONE,
    INVALID_AMOUNT,
    INVALID_ACCOUNT,
    INSUFFICIENT_BALANCE,
    DAILY_LIMIT_EXCEEDED,
    SELF_TRANSFER
}
