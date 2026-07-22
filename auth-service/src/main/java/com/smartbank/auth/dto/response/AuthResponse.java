package com.smartbank.auth.dto.response;

import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Response for login/refresh
@Getter
@Setter
@NoArgsConstructor
public class AuthResponse {

    private String token;             // access token
    private String refreshToken;      // refresh token (null on /refresh if not rotated)
    private String type = "Bearer";
    private String customerId;
    private String username;
    private String email;
    private Set<String> roles;
    private long expiresIn;           // access-token lifetime in ms

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
}
