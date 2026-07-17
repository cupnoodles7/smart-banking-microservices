package com.smartbank.transaction.entity;

// What kind of entity received the money (PRD sec 6.5).
public enum ReceiverType {
    ACCOUNT,  // a bank account
    WALLET,   // a digital wallet
    MERCHANT  // an external merchant (bill payments)
}
