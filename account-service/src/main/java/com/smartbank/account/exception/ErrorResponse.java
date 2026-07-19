package com.smartbank.account.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// Standard error body returned by the global handler (PRD sec 6.14).
// { "timestamp": "", "status": 400, "error": "", "message": "", "path": "" }
@Data
@Builder
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
