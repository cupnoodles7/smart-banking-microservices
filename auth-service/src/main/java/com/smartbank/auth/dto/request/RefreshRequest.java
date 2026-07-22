package com.smartbank.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//Refresh request to exchange a refresh token for a new access token.
@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
