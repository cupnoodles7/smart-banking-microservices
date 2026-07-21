package com.smartbank.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "security.internal")
public class InternalApiProperties {

    /**
     * Shared secret the caller must present in the {@code X-Internal-Api-Key} header.
     
     */
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
