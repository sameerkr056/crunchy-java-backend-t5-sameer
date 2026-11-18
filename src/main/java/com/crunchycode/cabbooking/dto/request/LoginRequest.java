package com.crunchycode.cabbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Your username", example = "student1")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Your password", example = "StrongPass1")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
