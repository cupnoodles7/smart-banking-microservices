package com.smartbank.transaction.constants;

// Shared constants for the Transaction Service.
public final class TransactionConstants {

    private TransactionConstants() {
    } // utility class - no instances

    public static final String DEFAULT_CURRENCY = "INR"; // only currency in this MVP (PRD sec 6.5)

    public static final int DEFAULT_PAGE = 0;  // first page (PRD sec 6.7)
    public static final int DEFAULT_SIZE = 20; // page size (PRD sec 6.7)

    public static final String SORT_FIELD = "initiatedAt"; // list endpoints sort by this, descending

    public static final int MAX_LOOKUP_SIZE = 500; // cap when fetching a customer's accounts/wallets

    // Name of the header trusted services send on POST /transactions/internal (the secret
    // VALUE lives in config under security.internal.api-key, not here).
    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
}
