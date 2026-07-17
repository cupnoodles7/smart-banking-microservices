package com.smartbank.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT-related configuration bound from the {@code security.jwt.*} namespace.
 *
 * <p>The {@code secret} MUST match the secret the Auth Service signs tokens with,
 * so it is a natural candidate for centralized (shared) configuration.
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** HMAC signing secret (shared with the Auth Service). Must be >= 256 bits. */
    private String secret;

    /** Request paths that bypass JWT validation entirely (Ant-style patterns). */
    private List<String> openPaths = new ArrayList<>();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getOpenPaths() {
        return openPaths;
    }

    public void setOpenPaths(List<String> openPaths) {
        this.openPaths = openPaths;
    }
}
