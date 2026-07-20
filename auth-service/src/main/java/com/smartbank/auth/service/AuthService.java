package com.smartbank.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    //regsiter a new user
    public User registerUser(AuthRequest request) {
        logger.info("Registering user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String customerId = UUID.randomUUID().toString();

        // Create the customer profile in user-service FIRST, keyed on this same customerId.
        // If it fails we abort here and never persist the credential, so we don't leave an
        // auth record with no matching profile. (No cross-DB transaction exists; the inverse
        // risk - a profile with no credential - is only possible if the save below fails.)
        userServiceClient.createProfile(customerId, request);

        User user = new User();
        user.setCustomerId(customerId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        User saved = userRepository.save(user);
        logger.info("User registered: {} (customerId={})", saved.getUsername(), saved.getCustomerId());
        return saved;
    }

    //Authenticate and issue an access + refresh token pair
    public AuthResponse loginUser(LoginRequest request) {
        logger.info("Login attempt: {}", request.getUsernameOrEmail());

        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid username/email or password");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username/email or password");
        }
        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
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
            throw new RuntimeException("Invalid or expired refresh token");
        }

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not recognized"));

        if (user.getRefreshTokenExpiry() != null
                && user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
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
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }
}
