package com.smartbank.user.controller;

import com.smartbank.user.config.InternalApiProperties;
import com.smartbank.user.constants.UserServiceConstants;
import com.smartbank.user.dto.request.CreateUserRequest;
import com.smartbank.user.dto.request.UpdateUserRequest;
import com.smartbank.user.dto.response.UserResponse;
import com.smartbank.user.exception.ForbiddenException;
import com.smartbank.user.security.AuthenticatedCustomer;
import com.smartbank.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Customer profile management (PRD sec 6.7)")
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

    
     //Fetch a customer profile. JWT-protected at the Gateway; a caller may only read
     
    @GetMapping("/{id}")
    @Operation(summary = "Get a customer profile",
            description = "Fetches a profile by id. Self-access only: the gateway-injected "
                    + "X-Customer-Id header must match the path id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "403", description = "Caller may only access their own profile"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<UserResponse> getUser(
            @PathVariable String id,
            @RequestHeader(value = UserServiceConstants.HEADER_CUSTOMER_ID, required = false) String callerId) {
        authenticatedCustomer.authorizeSelfAccess(callerId, id);
        return ResponseEntity.ok(userService.getById(id));
    }

    
     // Update a customer profile. JWT-protected at the Gateway; a caller may only update
    @PutMapping("/{id}")
    @Operation(summary = "Update a customer profile",
            description = "Updates a profile by id. Self-access only: the gateway-injected "
                    + "X-Customer-Id header must match the path id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Caller may only update their own profile"),
            @ApiResponse(responseCode = "404", description = "Profile not found"),
            @ApiResponse(responseCode = "409", description = "Email or phone already in use")
    })
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @RequestHeader(value = UserServiceConstants.HEADER_CUSTOMER_ID, required = false) String callerId,
            @Valid @RequestBody UpdateUserRequest request) {
        authenticatedCustomer.authorizeSelfAccess(callerId, id);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PostMapping("/internal")
    @Operation(summary = "Create a customer profile (internal)",
            description = "Service-to-service endpoint the Auth Service calls during registration. "
                    + "Carries no JWT; guarded by the shared X-Internal-Api-Key header. The id is "
                    + "supplied by Auth so the profile _id equals the system-wide customerId.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profile created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid internal API key"),
            @ApiResponse(responseCode = "409", description = "Id, email, or phone already in use")
    })
    public ResponseEntity<UserResponse> createInternal(
            @RequestHeader(value = UserServiceConstants.HEADER_INTERNAL_API_KEY, required = false) String apiKey,
            @Valid @RequestBody CreateUserRequest request) {
        requireValidInternalKey(apiKey);
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Internal compensating delete: Auth calls this to remove an orphan profile if its
    // credential write fails after create. Idempotent (204 whether or not it existed).
    @DeleteMapping("/internal/{id}")
    @Operation(summary = "Delete a customer profile (internal)",
            description = "Compensating delete the Auth Service calls to remove an orphan profile "
                    + "when its credential write fails after create. Guarded by X-Internal-Api-Key. "
                    + "Idempotent: returns 204 whether or not the profile existed.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profile deleted or already absent"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid internal API key")
    })
    public ResponseEntity<Void> deleteInternal(
            @RequestHeader(value = UserServiceConstants.HEADER_INTERNAL_API_KEY, required = false) String apiKey,
            @PathVariable String id) {
        requireValidInternalKey(apiKey);
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void requireValidInternalKey(String apiKey) {
        String expected = internalApiProperties.getApiKey();
        if (!StringUtils.hasText(expected) || !expected.equals(apiKey)) {
            log.warn("Rejected internal profile-create: missing or invalid internal API key");
            throw new ForbiddenException("Invalid internal API key");
        }
    }
}
