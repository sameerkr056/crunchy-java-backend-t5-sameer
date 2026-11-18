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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests the ride business rules (status transitions and driver availability) without any database. */
@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;
    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private RideService rideService;

    private static Driver driver(Long id, boolean available) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setName("Driver " + id);
        driver.setAvailable(available);
        return driver;
    }

    private static Ride ride(Long id, RideStatus status, Driver driver) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerName("Passenger");
        ride.setPickupLocation("A");
        ride.setDropLocation("B");
        ride.setFare(new BigDecimal("100.00"));
        ride.setStatus(status);
        ride.setBookingTime(LocalDateTime.of(2026, 11, 1, 9, 0));
        driver.addRide(ride);
        return ride;
    }

    private static RideRequest request(Long driverId) {
        RideRequest request = new RideRequest();
        request.setPassengerName("New Passenger");
        request.setPickupLocation("Station");
        request.setDropLocation("Airport");
        request.setFare(new BigDecimal("150"));
        request.setDriverId(driverId);
        return request;
    }

    // ---------------- create ----------------

    @Test
    void createRide_alwaysStartsAsRequestedAndLinksTheDriver() {
        Driver driver = driver(1L, true);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        RideResponse response = rideService.createRide(request(1L));

        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(response.getDriverId()).isEqualTo(1L);
        assertThat(response.getFare()).isEqualByComparingTo("150.00");
        assertThat(response.getFare().scale()).isEqualTo(2);
        assertThat(response.getBookingTime()).isNotNull();
        assertThat(driver.getRides()).hasSize(1); // both sides of the relationship are set
        assertThat(driver.isAvailable()).isTrue(); // a new ride does not make the driver busy
    }

    @Test
    void createRide_missingDriver_throwsNotFoundAndSavesNothing() {
        when(driverRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.createRide(request(5L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver not found");
        verify(rideRepository, never()).save(any());
    }

    // ---------------- read ----------------

    @Test
    void getRideById_missing_throwsNotFound() {
        when(rideRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getRideById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllRides_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Ride existing = ride(1L, RideStatus.REQUESTED, driver(1L, true));
        when(rideRepository.search(RideStatus.REQUESTED, null, pageable))
                .thenReturn(new PageImpl<>(List.of(existing), pageable, 1));

        PageResponse<RideResponse> result = rideService.getAllRides(RideStatus.REQUESTED, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---------------- accept ----------------

    @Test
    void acceptRide_makesRideAcceptedAndDriverUnavailable() {
        Driver driver = driver(1L, true);
        Ride existing = ride(1L, RideStatus.REQUESTED, driver);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(existing));

        RideResponse response = rideService.acceptRide(1L);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(driver.isAvailable()).isFalse();
    }

    @Test
    void acceptRide_driverNotAvailable_throwsConflict() {
        Driver busyDriver = driver(1L, false);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.REQUESTED, busyDriver)));

        assertThatThrownBy(() -> rideService.acceptRide(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void acceptRide_onlyRequestedRidesCanBeAccepted() {
        Driver driver = driver(1L, true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.COMPLETED, driver)));

        assertThatThrownBy(() -> rideService.acceptRide(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Only REQUESTED");
        assertThat(driver.isAvailable()).isTrue();
    }

    // ---------------- complete / cancel ----------------

    @Test
    void updateRideStatus_complete_freesTheDriver() {
        Driver driver = driver(1L, false);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.ACCEPTED, driver)));

        RideResponse response = rideService.updateRideStatus(1L, RideStatus.COMPLETED);

        assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(driver.isAvailable()).isTrue();
    }

    @Test
    void updateRideStatus_cancel_freesTheDriver() {
        Driver driver = driver(1L, false);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.ACCEPTED, driver)));

        RideResponse response = rideService.updateRideStatus(1L, RideStatus.CANCELLED);

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(driver.isAvailable()).isTrue();
    }

    @Test
    void updateRideStatus_fromRequested_isInvalid() {
        Driver driver = driver(1L, true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.REQUESTED, driver)));

        assertThatThrownBy(() -> rideService.updateRideStatus(1L, RideStatus.COMPLETED))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateRideStatus_fromCompleted_isInvalid() {
        Driver driver = driver(1L, true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.COMPLETED, driver)));

        assertThatThrownBy(() -> rideService.updateRideStatus(1L, RideStatus.CANCELLED))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void updateRideStatus_acceptedToRequested_isInvalid() {
        Driver driver = driver(1L, false);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.ACCEPTED, driver)));

        assertThatThrownBy(() -> rideService.updateRideStatus(1L, RideStatus.REQUESTED))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(driver.isAvailable()).isFalse();
    }

    // ---------------- update ----------------

    @Test
    void updateRide_sameDriver_changesTheDetailsButNotTheStatus() {
        Driver driver = driver(1L, true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.REQUESTED, driver)));
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));

        RideResponse response = rideService.updateRide(1L, request(1L));

        assertThat(response.getPassengerName()).isEqualTo("New Passenger");
        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(driver.getRides()).hasSize(1);
    }

    @Test
    void updateRide_otherDriver_movesTheRideAndUpdatesBothSides() {
        Driver oldDriver = driver(1L, true);
        Driver newDriver = driver(2L, true);
        Ride existing = ride(1L, RideStatus.REQUESTED, oldDriver);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(newDriver));

        RideResponse response = rideService.updateRide(1L, request(2L));

        assertThat(response.getDriverId()).isEqualTo(2L);
        assertThat(oldDriver.getRides()).isEmpty();
        assertThat(newDriver.getRides()).containsExactly(existing);
    }

    @Test
    void updateRide_onlyRequestedRidesCanBeEdited() {
        Driver driver = driver(1L, false);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.ACCEPTED, driver)));

        assertThatThrownBy(() -> rideService.updateRide(1L, request(1L)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Only REQUESTED");
    }

    @Test
    void updateRide_missingNewDriver_throwsNotFound() {
        Driver driver = driver(1L, true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride(1L, RideStatus.REQUESTED, driver)));
        when(driverRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.updateRide(1L, request(2L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- delete ----------------

    @Test
    void deleteRide_acceptedRide_freesTheDriver() {
        Driver driver = driver(1L, false);
        Ride existing = ride(1L, RideStatus.ACCEPTED, driver);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(existing));

        rideService.deleteRide(1L);

        assertThat(driver.isAvailable()).isTrue();
        verify(rideRepository).delete(existing);
    }

    @Test
    void deleteRide_requestedRide_leavesTheDriverAlone() {
        Driver driver = driver(1L, true);
        Ride existing = ride(1L, RideStatus.REQUESTED, driver);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(existing));

        rideService.deleteRide(1L);

        assertThat(driver.isAvailable()).isTrue();
        verify(rideRepository).delete(existing);
    }

    @Test
    void deleteRide_missing_throwsNotFound() {
        when(rideRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.deleteRide(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(rideRepository, never()).delete(any());
    }
}
