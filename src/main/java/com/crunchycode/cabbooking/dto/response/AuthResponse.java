package com.crunchycode.cabbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthResponse {

    @Schema(description = "The JWT. Send it as: Authorization: Bearer <token>", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdHVkZW50MSJ9.signature")
    private final String token;
    private final String tokenType;
    @Schema(description = "Minutes until the token expires", example = "60")
    private final long expiresInMinutes;
    @Schema(example = "student1")
    private final String username;
    @Schema(description = "USER or ADMIN", example = "USER")
    private final String role;

    public AuthResponse(String token, long expiresInMinutes, String username, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresInMinutes = expiresInMinutes;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
