package com.smartbank.transaction.entity;

// Final outcome of a transaction - written once, never changed (PRD sec 6.5, item 1).
public enum TransactionStatus {
    SUCCESS, // money moved as intended
    FAILED   // a business rule blocked it (still recorded for audit)
}
