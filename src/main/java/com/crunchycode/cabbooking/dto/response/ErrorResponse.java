package com.crunchycode.cabbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public class ErrorResponse {

    @Schema(description = "When the error happened", example = "2026-11-17T10:15:30.123")
    private final LocalDateTime timestamp;
    @Schema(description = "HTTP status code", example = "404")
    private final int status;
    @Schema(description = "Short explanation (never a stack trace)", example = "Driver not found with id: 99")
    private final String message;
    @Schema(description = "The URL that was called", example = "/api/v1/drivers/99")
    private final String path;

    public ErrorResponse(int status, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
