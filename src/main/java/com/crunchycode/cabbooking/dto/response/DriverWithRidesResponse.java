package com.crunchycode.cabbooking.dto.response;

import com.crunchycode.cabbooking.model.Driver;

import java.util.List;

/**
 * A driver together with all of its rides.
 * Building this response touches driver.getRides(), which is what makes the N+1 problem visible.
 */
public class DriverWithRidesResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final String licenseNumber;
    private final boolean available;
    private final List<RideResponse> rides;

    private DriverWithRidesResponse(Driver driver, List<RideResponse> rides) {
        this.id = driver.getId();
        this.name = driver.getName();
        this.email = driver.getEmail();
        this.phone = driver.getPhone();
        this.licenseNumber = driver.getLicenseNumber();
        this.available = driver.isAvailable();
        this.rides = rides;
    }

    public static DriverWithRidesResponse from(Driver driver) {
        // driver.getRides() is LAZY: if the rides were not fetched earlier,
        // Hibernate runs one extra SELECT right here, once per driver.
        List<RideResponse> rides = driver.getRides().stream().map(RideResponse::from).toList();
        return new DriverWithRidesResponse(driver, rides);
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

    public List<RideResponse> getRides() {
        return rides;
    }
}
