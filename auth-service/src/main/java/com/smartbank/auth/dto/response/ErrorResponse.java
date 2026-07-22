package com.smartbank.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// Consistent error body shared across the platform's services:
// { timestamp, status, error, message, path }. 'error' is the reason-phrase code
// (e.g. "Conflict") and 'message' is the human-readable detail.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
