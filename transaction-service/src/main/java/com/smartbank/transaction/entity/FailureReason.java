package com.smartbank.transaction.entity;

// Why a FAILED transaction failed; NONE for successful ones (PRD sec 6.5).
public enum FailureReason {
    NONE,                 // used when status == SUCCESS
    INVALID_AMOUNT,       // amount <= 0
    INVALID_ACCOUNT,      // account not found / not usable
    INVALID_WALLET,       // wallet not found / not usable
    INSUFFICIENT_BALANCE, // debit exceeds available balance
    DAILY_LIMIT_EXCEEDED, // daily amount/count limit hit
    WALLET_LIMIT_EXCEEDED,// wallet max balance would be exceeded
    SELF_TRANSFER         // sender and receiver are the same (PRD sec 7.3)
}
