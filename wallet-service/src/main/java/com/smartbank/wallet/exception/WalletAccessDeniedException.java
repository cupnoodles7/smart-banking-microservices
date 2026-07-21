package com.smartbank.wallet.exception;

// Thrown when someone tries to touch a wallet that isn't theirs. Comes back as a 403 Forbidden.
public class WalletAccessDeniedException extends RuntimeException {

    public WalletAccessDeniedException(String message) {
        super(message);
    }
}
