package com.crunchycode.cabbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.crunchycode.cabbooking.model.Ride;
import com.crunchycode.cabbooking.model.RideStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RideResponse {

    @Schema(example = "12")
    private final Long id;
    private final String passengerName;
    private final String pickupLocation;
    private final String dropLocation;
    private final BigDecimal fare;
    @Schema(description = "REQUESTED, ACCEPTED, COMPLETED or CANCELLED", example = "REQUESTED")
    private final RideStatus status;
    @Schema(example = "3")
    private final Long driverId;
    private final LocalDateTime bookingTime;

    public RideResponse(Long id, String passengerName, String pickupLocation, String dropLocation,
                        BigDecimal fare, RideStatus status, Long driverId, LocalDateTime bookingTime) {
        this.id = id;
        this.passengerName = passengerName;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.fare = fare;
        this.status = status;
        this.driverId = driverId;
        this.bookingTime = bookingTime;
    }

    public static RideResponse from(Ride ride) {
        return new RideResponse(ride.getId(), ride.getPassengerName(), ride.getPickupLocation(),
                ride.getDropLocation(), ride.getFare(), ride.getStatus(), ride.getDriver().getId(),
                ride.getBookingTime());
    }

    public Long getId() {
        return id;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public String getDropLocation() {
        return dropLocation;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public RideStatus getStatus() {
        return status;
    }

    public Long getDriverId() {
        return driverId;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }
}
