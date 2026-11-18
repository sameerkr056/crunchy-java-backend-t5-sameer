package com.crunchycode.cabbooking.service;

import com.crunchycode.cabbooking.dto.request.RideRequest;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.RideResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.exception.ResourceNotFoundException;
import com.crunchycode.cabbooking.model.Driver;
import com.crunchycode.cabbooking.model.Ride;
import com.crunchycode.cabbooking.model.RideStatus;
import com.crunchycode.cabbooking.repository.DriverRepository;
import com.crunchycode.cabbooking.repository.RideRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class RideService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    public RideService(RideRepository rideRepository, DriverRepository driverRepository) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public RideResponse createRide(RideRequest request) {
        // Rule 1: a ride must belong to an existing driver
        Driver driver = findDriverOrThrow(request.getDriverId());

        Ride ride = new Ride();
        copyDetails(request, ride);
        // Rules 3 and 4: always REQUESTED, the client cannot choose
        ride.setStatus(RideStatus.REQUESTED);
        ride.setBookingTime(LocalDateTime.now());
        driver.addRide(ride); // sets both sides of the relationship
        return RideResponse.from(rideRepository.save(ride));
    }

    public PageResponse<RideResponse> getAllRides(RideStatus status, Long driverId, Pageable pageable) {
        Page<RideResponse> page = rideRepository.search(status, driverId, pageable).map(RideResponse::from);
        return PageResponse.from(page);
    }

    public RideResponse getRideById(Long id) {
        return RideResponse.from(findRideOrThrow(id));
    }

    // Rule 10: PUT never changes the status. Only REQUESTED rides can be edited.
    @Transactional
    public RideResponse updateRide(Long id, RideRequest request) {
        Ride ride = findRideOrThrow(id);
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new ResourceConflictException(
                    "Only REQUESTED rides can be updated. Current status: " + ride.getStatus());
        }

        Driver newDriver = findDriverOrThrow(request.getDriverId());
        if (!newDriver.getId().equals(ride.getDriver().getId())) {
            // moving the ride to another driver: update both sides
            ride.getDriver().removeRide(ride);
            newDriver.addRide(ride);
        }
        copyDetails(request, ride);
        return RideResponse.from(ride);
    }

    @Transactional
    public void deleteRide(Long id) {
        Ride ride = findRideOrThrow(id);
        // If an accepted ride is deleted, its driver must not stay unavailable forever
        if (ride.getStatus() == RideStatus.ACCEPTED) {
            ride.getDriver().setAvailable(true);
        }
        rideRepository.delete(ride);
    }

    // Rules 2, 5 and 6
    @Transactional
    public RideResponse acceptRide(Long id) {
        Ride ride = findRideOrThrow(id);
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new ResourceConflictException(
                    "Only REQUESTED rides can be accepted. Current status: " + ride.getStatus());
        }

        Driver driver = ride.getDriver();
        if (!driver.isAvailable()) {
            throw new ResourceConflictException(
                    "Driver with id " + driver.getId() + " is not available to accept this ride");
        }

        // Both changes are saved together when the transaction commits
        ride.setStatus(RideStatus.ACCEPTED);
        driver.setAvailable(false);
        return RideResponse.from(ride);
    }

    // Rules 7, 8 and 9
    @Transactional
    public RideResponse updateRideStatus(Long id, RideStatus newStatus) {
        Ride ride = findRideOrThrow(id);
        RideStatus currentStatus = ride.getStatus();

        boolean newStatusAllowed = newStatus == RideStatus.COMPLETED || newStatus == RideStatus.CANCELLED;
        if (currentStatus != RideStatus.ACCEPTED || !newStatusAllowed) {
            throw new ResourceConflictException("Invalid status transition from " + currentStatus + " to "
                    + newStatus + ". Only ACCEPTED rides can become COMPLETED or CANCELLED"
                    + " (use the accept endpoint to accept a REQUESTED ride)");
        }

        ride.setStatus(newStatus);
        ride.getDriver().setAvailable(true);
        return RideResponse.from(ride);
    }

    private void copyDetails(RideRequest request, Ride ride) {
        ride.setPassengerName(request.getPassengerName());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropLocation(request.getDropLocation());
        ride.setFare(request.getFare().setScale(2, RoundingMode.HALF_UP));
    }

    private Ride findRideOrThrow(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
    }

    private Driver findDriverOrThrow(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }
}
