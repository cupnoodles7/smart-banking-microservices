package com.smartbank.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Shared secret trusted services send in X-Internal-Api-Key to write to the ledger.
// Bound from security.internal.api-key.
@ConfigurationProperties(prefix = "security.internal")
public class InternalApiProperties {

    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
