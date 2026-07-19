package com.smartbank.auth.dto.response;

import java.util.Set;

//Response for login/refresh
public class AuthResponse {

    private String token;             // access token
    private String refreshToken;      // refresh token (null on /refresh if not rotated)
    private String type = "Bearer";
    private String customerId;
    private String username;
    private String email;
    private Set<String> roles;
    private long expiresIn;           // access-token lifetime in ms

    public AuthResponse() {
    }

    public AuthResponse(String token, String refreshToken, String customerId, String username,
                        String email, Set<String> roles, long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.customerId = customerId;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
