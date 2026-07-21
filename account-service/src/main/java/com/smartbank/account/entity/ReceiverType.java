package com.smartbank.account.entity;

// Party type on the receiving side of a recorded transaction (PRD sec 6.7). Account
// Service only ever moves money into accounts, so this is always ACCOUNT.
public enum ReceiverType {
    ACCOUNT
}
