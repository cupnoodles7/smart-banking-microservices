package com.smartbank.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the internal (service-to-service) surface of the User Service,
 * bound from the {@code security.internal.*} namespace.
 *
 * <p>Mirrors the Gateway's {@code JwtProperties} style. The {@code apiKey} guards
 * {@code POST /users/internal}, which the Auth Service calls during registration.
 */
@ConfigurationProperties(prefix = "security.internal")
public class InternalApiProperties {

    /**
     * Shared secret the caller must present in the {@code X-Internal-Api-Key} header.
     * Placeholder internal-auth scheme (see {@code UserController#createInternal}).
     */
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
