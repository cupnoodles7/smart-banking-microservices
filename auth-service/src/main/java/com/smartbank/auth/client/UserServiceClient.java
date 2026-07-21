package com.smartbank.auth.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.smartbank.auth.dto.request.AuthRequest;

//Calls the User Service's internal, service-to-service profile endpoints during registration
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalApiKey;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${user-service.base-url:http://user-service}") String baseUrl,
            @Value("${user-service.internal-api-key:${security.internal.api-key:${USER_INTERNAL_API_KEY}}}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.internalApiKey = internalApiKey;
    }

    //Creates the customer profile. The generated {@code customerId} is passed as the profile id so the User document {@code _id} equals the system-wide customerId.
    
    @SuppressWarnings("unchecked")
    public String createProfile(String customerId, AuthRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", customerId);
        body.put("fullName", request.getFullName());
        body.put("email", request.getEmail());
        body.put("phoneNumber", request.getPhoneNumber());
        body.put("address", request.getAddress());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", internalApiKey);

        Map<String, Object> created = restTemplate.postForObject(
                baseUrl + "/users/internal", new HttpEntity<>(body, headers), Map.class);

        String id = created != null ? String.valueOf(created.get("id")) : customerId;
        log.info("Created user profile {} for username {}", id, request.getUsername());
        return id;
    }

    
    public void deleteProfileQuietly(String customerId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            restTemplate.exchange(baseUrl + "/users/internal/" + customerId,
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            log.info("Compensated: deleted orphan profile {}", customerId);
        } catch (Exception ex) {
            log.error("Compensation FAILED: could not delete orphan profile {} - needs manual "
                    + "reconciliation: {}", customerId, ex.getMessage());
        }
    }
}
