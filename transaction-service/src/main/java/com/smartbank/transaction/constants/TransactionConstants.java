package com.smartbank.transaction.constants;

// Shared constants for the Transaction Service.
public final class TransactionConstants {

    private TransactionConstants() {
    } // utility class - no instances

    public static final String DEFAULT_CURRENCY = "INR"; 

    public static final int DEFAULT_PAGE = 0;  
    public static final int DEFAULT_SIZE = 20; 

    public static final String SORT_FIELD = "initiatedAt"; 

    public static final int MAX_LOOKUP_SIZE = 500; 
}
