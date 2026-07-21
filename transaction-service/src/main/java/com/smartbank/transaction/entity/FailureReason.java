package com.smartbank.transaction.entity;

public enum FailureReason {
    NONE,                
    INVALID_AMOUNT,      
    INVALID_ACCOUNT,      
    INVALID_WALLET,       
    INSUFFICIENT_BALANCE, 
    DAILY_LIMIT_EXCEEDED, 
    WALLET_LIMIT_EXCEEDED,
    SELF_TRANSFER         
}
