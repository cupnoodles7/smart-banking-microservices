package com.smartbank.auth.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

//customerId is the stable identity that downstream services (account/wallet/transaction) key on; it is generated once at registration.
@Getter
@Setter
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

    // Explicit, password-safe toString (never log the hash or refresh token).
    @Override
    public String toString() {
        return "User [id=" + id + ", customerId=" + customerId + ", username=" + username
                + ", email=" + email + ", roles=" + roles + ", enabled=" + enabled
                + ", createdAt=" + createdAt + ", lastLogin=" + lastLogin + "]";
    }
}
