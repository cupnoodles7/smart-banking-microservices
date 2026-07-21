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

import jakarta.validation.Valid;

//register/login/refresh are open at the Gateway
//everything else is protected by the Gateway and receives identity via X-Auth headers

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;


    //Public endpoints (open at the Gateway)
    @PostMapping("/register")
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        logger.info("Refresh token request");
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }


    //kept for manual testing and potential internal use.
    @PostMapping("/validate")
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
    public ResponseEntity<User> getCurrentUser(@RequestHeader("X-Auth-Username") String username) {
        logger.info("Profile request: {}", username);
        return ResponseEntity.ok(authService.getUserByUsername(username));
    }

    @GetMapping("/users")
    public ResponseEntity<Iterable<User>> getAllUsers(
            @RequestHeader(value = "X-Auth-Roles", required = false) String rolesHeader) {
        if (rolesHeader == null || !rolesHeader.contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required.");
        }
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // Stateless JWT: logout is client-side (discard the token)
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful. Please discard your token.");
        return ResponseEntity.ok(response);
    }
}
