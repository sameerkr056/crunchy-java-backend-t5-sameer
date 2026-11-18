package com.crunchycode.cabbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.crunchycode.cabbooking.model.Driver;

public class DriverResponse {

    @Schema(example = "3")
    private final Long id;
    @Schema(example = "Suresh Mahato")
    private final String name;
    private final String email;
    private final String phone;
    private final String licenseNumber;
    @Schema(description = "false while the driver is on an ACCEPTED ride", example = "true")
    private final boolean available;

    public DriverResponse(Long id, String name, String email, String phone,
                          String licenseNumber, boolean available) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.available = available;
    }

    public static DriverResponse from(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getName(), driver.getEmail(),
                driver.getPhone(), driver.getLicenseNumber(), driver.isAvailable());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public boolean isAvailable() {
        return available;
    }
}
