package com.smartbank.transaction.security;

import com.smartbank.transaction.config.InternalApiProperties;
import com.smartbank.transaction.exception.UnauthorizedInternalAccessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalApiKeyGuardTest {

    private static InternalApiKeyGuard guardWithKey(String configuredKey) {
        InternalApiProperties props = new InternalApiProperties();
        props.setApiKey(configuredKey);
        return new InternalApiKeyGuard(props);
    }

    @Test
    void acceptsMatchingKey() {
        InternalApiKeyGuard guard = guardWithKey("secret-key");
        assertDoesNotThrow(() -> guard.verify("secret-key"));
    }

    @Test
    void rejectsWrongKey() {
        InternalApiKeyGuard guard = guardWithKey("secret-key");
        assertThrows(UnauthorizedInternalAccessException.class, () -> guard.verify("wrong-key"));
    }

    @Test
    void rejectsMissingKey() {
        InternalApiKeyGuard guard = guardWithKey("secret-key");
        assertThrows(UnauthorizedInternalAccessException.class, () -> guard.verify(null));
        assertThrows(UnauthorizedInternalAccessException.class, () -> guard.verify("  "));
    }

    @Test
    void failsClosedWhenServerHasNoKeyConfigured() {
        // A blank server-side key must reject every call rather than accepting anything.
        InternalApiKeyGuard blank = guardWithKey("");
        assertThrows(UnauthorizedInternalAccessException.class, () -> blank.verify(""));
        assertThrows(UnauthorizedInternalAccessException.class, () -> blank.verify("anything"));

        InternalApiKeyGuard nullKey = guardWithKey(null);
        assertThrows(UnauthorizedInternalAccessException.class, () -> nullKey.verify("anything"));
    }
}
