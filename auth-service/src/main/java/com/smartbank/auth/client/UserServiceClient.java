package com.smartbank.auth.client;

import com.smartbank.auth.dto.request.AuthRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls user-service's internal profile-create endpoint during registration.
 *
 * <p>The endpoint is not exposed through the Gateway (the customer has no JWT yet), so we
 * reach it directly via Eureka ({@code lb://user-service}) using the load-balanced
 * {@link RestTemplate}, presenting the shared {@code X-Internal-Api-Key}.
 */
@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalApiKey;

    public UserServiceClient(RestTemplate restTemplate,
                             @Value("${user-service.url:http://user-service}") String baseUrl,
                             @Value("${security.internal.api-key:}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.internalApiKey = internalApiKey;
    }

    /**
     * Create the customer profile with {@code _id == customerId}. Throws if user-service
     * rejects the request or is unreachable, so the caller can abort registration before
     * persisting the credential.
     */
    public void createProfile(String customerId, AuthRequest request) {
        ProfileCreateRequest body = new ProfileCreateRequest(
                customerId,
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getAddress());

        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_API_KEY_HEADER, internalApiKey);

        String url = baseUrl + "/users/internal";
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
            logger.info("Created user-service profile for customerId={}", customerId);
        } catch (RestClientResponseException ex) {
            // user-service returned a 4xx/5xx (e.g. duplicate email/phone, bad input).
            logger.error("user-service rejected profile create for customerId={}: {} {}",
                    customerId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException(
                    "Failed to create user profile: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            // Connectivity / Eureka lookup failure - user-service not reachable.
            logger.error("user-service unreachable while creating profile for customerId={}",
                    customerId, ex);
            throw new RuntimeException("User service unavailable; registration aborted", ex);
        }
    }
}
