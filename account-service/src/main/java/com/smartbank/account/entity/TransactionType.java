package com.smartbank.account.entity;

// Kinds of money movement Account Service can report to Transaction Service's
// immutable ledger via POST /transactions/internal (PRD sec 6.7). Mirrors the
// subset of Transaction Service's own TransactionType enum that Account Service
// ever produces; Wallet/Bill Payment types belong to other services.
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER
}
