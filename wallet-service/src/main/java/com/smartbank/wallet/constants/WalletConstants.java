package com.smartbank.wallet.constants;

// A few fixed values the wallet needs (currency, retry counts, header names). The money limits live in config, not here.
public final class WalletConstants {

    private WalletConstants() {
    }

    // Everything here is in Indian rupees.
    public static final String CURRENCY = "INR";

    // How many times we retry a wallet update that lost a race before giving up with a 409.
    public static final int MAX_WRITE_RETRIES = 3;

    // How many times we retry refunding the account when a top-up couldn't be finished.
    public static final int MAX_REVERSAL_RETRIES = 3;

    // Identity headers the API Gateway adds once it has checked the login token.
    public static final String HEADER_CUSTOMER_ID = "X-Customer-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
}
