package com.crunchycode.cabbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DriverRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    @Schema(description = "Full name", example = "Ravi Kumar")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    @Schema(description = "Unique email address", example = "ravi.kumar@example.com")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Phone must contain 10 to 13 digits, optionally starting with +")
    @Schema(description = "10 to 13 digits, optional leading +", example = "9876543210")
    private String phone;

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must be at most 50 characters")
    @Schema(description = "Unique license number", example = "JH0120230001")
    private String licenseNumber;

    // Optional. If missing, a new driver is available by default.
    @Schema(description = "Optional. Defaults to true for a new driver", example = "true")
    private Boolean available;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
