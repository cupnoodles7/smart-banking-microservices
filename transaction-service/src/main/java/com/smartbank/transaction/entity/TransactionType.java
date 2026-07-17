package com.smartbank.transaction.entity;

// Kind of money movement being recorded (PRD sec 6.5).
public enum TransactionType {
    DEPOSIT,         // cash into an account
    WITHDRAW,        // cash out of an account
    TRANSFER,        // account -> account
    WALLET_TOPUP,    // account -> wallet
    WALLET_TRANSFER, // wallet -> wallet
    BILL_PAYMENT     // wallet -> merchant
}
