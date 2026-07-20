package com.smartbank.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

import com.smartbank.auth.client.UserServiceClient;
import com.smartbank.auth.dto.request.AuthRequest;
import com.smartbank.auth.dto.request.LoginRequest;
import com.smartbank.auth.dto.response.AuthResponse;
import com.smartbank.auth.entity.User;
import com.smartbank.auth.repository.UserRepository;
import com.smartbank.auth.util.JwtUtil;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserServiceClient userServiceClient;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    //Public

    // Register a new user
    //  1. reject duplicate username/email up front (auth side)
    //  2. create the customer profile in the User Service first, it owns the customerId
    //  3. write our credential row keyed on that customerId
    //  4. if step 3 fails, best-effort DELETE the profile so we don't orphan it
    public User registerUser(AuthRequest request) {
        logger.info("Registering user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // Step 2 - create the profile first
        String customerId = UUID.randomUUID().toString();
        try {
            customerId = userServiceClient.createProfile(customerId, request);
        } catch (HttpStatusCodeException ex) {
            logger.warn("Profile creation rejected by user-service: {}", ex.getStatusText());
            throw new ResponseStatusException(HttpStatus.valueOf(ex.getStatusCode().value()),
                    "Could not create profile: " + ex.getResponseBodyAsString());
        }

        // Step 3 - write the credential
        // step 4 - compensate on failure.
        try {
            User user = new User();
            user.setCustomerId(customerId);
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEnabled(true);

            User saved = userRepository.save(user);
            logger.info("User registered: {} (customerId={})", saved.getUsername(), saved.getCustomerId());
            return saved;
        } catch (RuntimeException ex) {
            logger.error("Credential write failed for customerId={}; compensating", customerId, ex);
            userServiceClient.deleteProfileQuietly(customerId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Registration failed while saving credentials");
        }
    }

    //Authenticate and issue an access + refresh token pair
    public AuthResponse loginUser(LoginRequest request) {
        logger.info("Login attempt: {}", request.getUsernameOrEmail());

        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        user.setLastLogin(LocalDateTime.now());

        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getCustomerId(), user.getEmail(), user.getRoles());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getCustomerId());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now()
                .plusNanos(jwtUtil.getRefreshTokenExpiryMs() * 1_000_000));
        userRepository.save(user);

        logger.info("Login successful: {}", user.getUsername());
        return new AuthResponse(
                accessToken, refreshToken, user.getCustomerId(),
                user.getUsername(), user.getEmail(), user.getRoles(),
                jwtUtil.getAccessTokenExpiryMs());
    }

    //Exchange a valid, stored refresh token for a new access token
    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)
                || !"refresh".equals(jwtUtil.extractTokenType(refreshToken))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not recognized"));

        if (user.getRefreshTokenExpiry() != null
                && user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getCustomerId(), user.getEmail(), user.getRoles());

        logger.info("Access token refreshed: {}", user.getUsername());
        return new AuthResponse(
                accessToken, null, user.getCustomerId(),
                user.getUsername(), user.getEmail(), user.getRoles(),
                jwtUtil.getAccessTokenExpiryMs());
    }


    //Private

    public User getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }
}
