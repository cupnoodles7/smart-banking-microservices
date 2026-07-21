package com.smartbank.auth.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

//customerId is the stable identity that downstream services (account/wallet/transaction) key on; it is generated once at registration.
@Document(collection = "users")
public class User {

    @Id
    private String id;

    
    @Indexed(unique = true)
    private String customerId;

    @Indexed(unique = true) // unique login handle
    private String username;

    @Indexed(unique = true) // unique email
    private String email;

    @JsonIgnore // never serialize the BCrypt hash to any client (PRD sec 6.10 / DTO discipline)
    private String password;

    private Set<String> roles = new HashSet<>(); // USER, ADMIN

    private boolean enabled = true;

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

   
    @JsonIgnore // stored refresh token must never leak to a client
    private String refreshToken;

    @JsonIgnore
    private LocalDateTime refreshTokenExpiry;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.roles.add("USER");
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
