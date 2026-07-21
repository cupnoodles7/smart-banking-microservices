package com.smartbank.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

//Refresh request to exchange a refresh token for a new access token.
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
