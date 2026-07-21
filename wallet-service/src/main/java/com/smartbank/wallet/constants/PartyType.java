package com.smartbank.wallet.constants;

/**
 * Sender / receiver party type on a ledger entry (PRD §6.5).
 * A wallet may send to another WALLET or to a MERCHANT (bill payment),
 * and receives from an ACCOUNT (top-up) or a WALLET (transfer).
 */
public enum PartyType {
    ACCOUNT,
    WALLET,
    MERCHANT
}
