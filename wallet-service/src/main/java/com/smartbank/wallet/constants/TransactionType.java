package com.smartbank.wallet.constants;

// The kinds of transactions the ledger tracks; the wallet only ever creates the wallet-related ones.
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER,
    WALLET_TOPUP,
    WALLET_TRANSFER,
    BILL_PAYMENT
}
