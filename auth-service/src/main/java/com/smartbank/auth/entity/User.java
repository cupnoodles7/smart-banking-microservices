package com.smartbank.auth.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Credentials record in auth_db (PRD sec 6.5).
 *
 * <p>{@code customerId} is the stable identity that downstream services
 * (account/wallet/transaction) key on; it is generated once at registration.
 * The customer's profile (name, phone, address) lives in the User Service, not here.
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    /** Stable cross-service identity (PRD sec 6.5). Generated at registration. */
    @Indexed(unique = true)
    private String customerId;

    @Indexed(unique = true) // unique login handle
    private String username;

    @Indexed(unique = true) // unique email
    private String email;

    private String password;

    private Set<String> roles = new HashSet<>(); // USER, ADMIN

    private boolean enabled = true;

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    // ---- Refresh-token support (PRD sec 6.8) ----
    private String refreshToken;

    private LocalDateTime refreshTokenExpiry;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.roles.add("USER"); // Default role
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", customerId=" + customerId + ", username=" + username
                + ", email=" + email + ", roles=" + roles + ", enabled=" + enabled
                + ", createdAt=" + createdAt + ", lastLogin=" + lastLogin + "]";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    public void setRefreshTokenExpiry(LocalDateTime refreshTokenExpiry) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }
}
