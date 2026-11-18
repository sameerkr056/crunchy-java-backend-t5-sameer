package com.crunchycode.cabbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username can only contain letters, digits, dot, underscore and hyphen")
    @Schema(description = "3 to 50 letters, digits, dot, underscore or hyphen", example = "student1")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    @Schema(description = "Unique email address", example = "student1@example.com")
    private String email;

    // BCrypt only uses the first 72 bytes, so longer passwords are not allowed
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
    @Schema(description = "8 to 72 characters", example = "StrongPass1")
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
