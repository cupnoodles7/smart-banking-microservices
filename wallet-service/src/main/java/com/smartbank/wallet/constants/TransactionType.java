package com.smartbank.wallet.constants;

/**
 * Transaction ledger types (PRD §6.5). The wallet service only ever writes the
 * wallet-related types, but the full enum mirrors the Transaction Service contract.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER,
    WALLET_TOPUP,
    WALLET_TRANSFER,
    BILL_PAYMENT
}
