package com.smartbank.auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartbank.auth.dto.request.AuthRequest;
import com.smartbank.auth.dto.request.LoginRequest;
import com.smartbank.auth.dto.request.RefreshRequest;
import com.smartbank.auth.dto.response.AuthResponse;
import com.smartbank.auth.entity.User;
import com.smartbank.auth.service.AuthService;
import com.smartbank.auth.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

//register/login/refresh are open at the Gateway
//everything else is protected by the Gateway and receives identity via X-Auth headers

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;


    //Public endpoints (open at the Gateway)
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AuthRequest request) {
        logger.info("Register request: {}", request.getUsername());
        User user = authService.registerUser(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("customerId", user.getCustomerId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("roles", user.getRoles());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "401")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "401")
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        logger.info("Refresh token request");
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }


    //kept for manual testing and potential internal use.
    @PostMapping("/validate")
    @Operation(summary = "Validate a token")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401")
    })
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("valid", false);
            response.put("message", "Invalid token format");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            response.put("valid", true);
            response.put("username", jwtUtil.extractUsername(token));
            response.put("customerId", jwtUtil.extractCustomerId(token));
            response.put("roles", jwtUtil.extractRoles(token));
            return ResponseEntity.ok(response);
        }
        response.put("valid", false);
        response.put("message", "Invalid or expired token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

   
//Private endpoints
    @GetMapping("/me")
    @Operation(summary = "Get current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<User> getCurrentUser(@RequestHeader("X-Auth-Username") String username) {
        logger.info("Profile request: {}", username);
        return ResponseEntity.ok(authService.getUserByUsername(username));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "403")
    })
    public ResponseEntity<Iterable<User>> getAllUsers(
            @RequestHeader(value = "X-Auth-Roles", required = false) String rolesHeader) {
        if (!hasRole(rolesHeader, "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required.");
        }
        return ResponseEntity.ok(authService.getAllUsers());
    }

    private static boolean hasRole(String rolesHeader, String role) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        for (String r : rolesHeader.split(",")) {
            if (r.trim().equals(role)) {
                return true;
            }
        }
        return false;
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out")
    @ApiResponses({
            @ApiResponse(responseCode = "200")
    })
    public ResponseEntity<Map<String, String>> logout() {
        // Stateless JWT: logout is client-side (discard the token)
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful. Please discard your token.");
        return ResponseEntity.ok(response);
    }
}
