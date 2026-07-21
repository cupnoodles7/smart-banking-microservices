package com.smartbank.transaction.security;

import com.smartbank.transaction.config.InternalApiProperties;
import com.smartbank.transaction.exception.UnauthorizedInternalAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Checks the shared secret on the internal ledger-write path. The gateway only checks the
// JWT, so this is what stops an end user from forging transactions.
@Component
public class InternalApiKeyGuard {

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyGuard.class);

    private final InternalApiProperties properties;

    public InternalApiKeyGuard(InternalApiProperties properties) {
        this.properties = properties;
    }

    public void verify(String presentedKey) {
        String expected = properties.getApiKey();

        // No key configured - reject rather than let everyone in.
        if (!StringUtils.hasText(expected)) {
            log.error("No internal API key configured - rejecting internal ledger write");
            throw new UnauthorizedInternalAccessException("Internal write path is not available");
        }

        if (!StringUtils.hasText(presentedKey) || !constantTimeEquals(expected, presentedKey)) {
            log.warn("Rejected internal ledger write: missing or invalid X-Internal-Api-Key header");
            throw new UnauthorizedInternalAccessException("Invalid or missing internal API key");
        }
    }

    // Timing-safe compare so a mismatch doesn't leak the key.
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
