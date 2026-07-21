package com.smartbank.wallet.constants;

// The reason a wallet transaction failed. NONE just means it went through fine.
public enum FailureReason {
    NONE,
    INVALID_AMOUNT,
    INVALID_ACCOUNT,
    INVALID_WALLET,
    INSUFFICIENT_BALANCE,
    DAILY_LIMIT_EXCEEDED,
    WALLET_LIMIT_EXCEEDED,
    SELF_TRANSFER
}
