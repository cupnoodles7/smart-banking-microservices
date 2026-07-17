package com.smartbank.transaction.entity;

// What kind of entity initiated the money movement (PRD sec 6.5).
public enum SenderType {
    ACCOUNT, // a bank account
    WALLET   // a digital wallet
}
