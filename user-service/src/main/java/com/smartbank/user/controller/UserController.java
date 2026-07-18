package com.smartbank.user.controller;

import com.smartbank.user.config.InternalApiProperties;
import com.smartbank.user.constants.UserServiceConstants;
import com.smartbank.user.dto.request.CreateUserRequest;
import com.smartbank.user.dto.request.UpdateUserRequest;
import com.smartbank.user.dto.response.UserResponse;
import com.smartbank.user.exception.ForbiddenException;
import com.smartbank.user.security.AuthenticatedCustomer;
import com.smartbank.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-profile endpoints (PRD sec 6.7). Controllers only orchestrate: authorize,
 * delegate to the service, and return a Response DTO - never the entity (PRD sec 6.10).
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthenticatedCustomer authenticatedCustomer;
    private final InternalApiProperties internalApiProperties;

    public UserController(UserService userService,
                          AuthenticatedCustomer authenticatedCustomer,
                          InternalApiProperties internalApiProperties) {
        this.userService = userService;
        this.authenticatedCustomer = authenticatedCustomer;
        this.internalApiProperties = internalApiProperties;
    }

    /**
     * Fetch a customer profile. JWT-protected at the Gateway; a caller may only read
     * their own profile (the {@code X-Customer-Id} header must match {@code {id}}).
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable String id,
            @RequestHeader(value = UserServiceConstants.HEADER_CUSTOMER_ID, required = false) String callerId) {
        authenticatedCustomer.authorizeSelfAccess(callerId, id);
        return ResponseEntity.ok(userService.getById(id));
    }

    /**
     * Update a customer profile. JWT-protected at the Gateway; a caller may only update
     * their own profile (the {@code X-Customer-Id} header must match {@code {id}}).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @RequestHeader(value = UserServiceConstants.HEADER_CUSTOMER_ID, required = false) String callerId,
            @Valid @RequestBody UpdateUserRequest request) {
        authenticatedCustomer.authorizeSelfAccess(callerId, id);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Internal, service-to-service endpoint: the Auth Service calls this to create a
     * profile during registration. It carries no JWT (the customer has none yet), so it
     * is not exposed through the Gateway's authenticated routes - internal callers reach
     * the service directly via Eureka ({@code lb://user-service}).
     *
     * <p>No repo-wide service-to-service auth convention exists yet, so this uses a
     * simple shared internal API-key header ({@code X-Internal-Api-Key}) as a
     * placeholder. Replace with the platform's chosen internal-auth mechanism (e.g. mTLS
     * or a signed service token) once one is defined.
     */
    @PostMapping("/internal")
    public ResponseEntity<UserResponse> createInternal(
            @RequestHeader(value = UserServiceConstants.HEADER_INTERNAL_API_KEY, required = false) String apiKey,
            @Valid @RequestBody CreateUserRequest request) {
        requireValidInternalKey(apiKey);
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private void requireValidInternalKey(String apiKey) {
        String expected = internalApiProperties.getApiKey();
        if (!StringUtils.hasText(expected) || !expected.equals(apiKey)) {
            log.warn("Rejected internal profile-create: missing or invalid internal API key");
            throw new ForbiddenException("Invalid internal API key");
        }
    }
}
