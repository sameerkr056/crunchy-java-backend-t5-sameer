package com.crunchycode.cabbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Note: there is no status field here on purpose.
 * Clients can never choose the ride status.
 */
public class RideRequest {

    @NotBlank(message = "Passenger name is required")
    @Size(max = 100, message = "Passenger name must be at most 100 characters")
    @Schema(description = "Name of the passenger", example = "Neha Gupta")
    private String passengerName;

    @NotBlank(message = "Pickup location is required")
    @Size(max = 255, message = "Pickup location must be at most 255 characters")
    @Schema(description = "Pickup address", example = "Dhanbad Railway Station")
    private String pickupLocation;

    @NotBlank(message = "Drop location is required")
    @Size(max = 255, message = "Drop location must be at most 255 characters")
    @Schema(description = "Drop address", example = "Hirapur")
    private String dropLocation;

    @NotNull(message = "Fare is required")
    @Positive(message = "Fare must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Fare can have at most 8 digits and 2 decimal places")
    @Schema(description = "Fare, greater than 0, at most 2 decimals", example = "150.00")
    private BigDecimal fare;

    @NotNull(message = "Driver id is required")
    @Positive(message = "Driver id must be a positive number")
    @Schema(description = "Id of an existing driver", example = "3")
    private Long driverId;

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDropLocation() {
        return dropLocation;
    }

    public void setDropLocation(String dropLocation) {
        this.dropLocation = dropLocation;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
}
