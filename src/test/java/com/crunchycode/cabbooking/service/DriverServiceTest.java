package com.crunchycode.cabbooking.service;

import com.crunchycode.cabbooking.dto.request.DriverRequest;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.DriverWithRidesResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.exception.ResourceNotFoundException;
import com.crunchycode.cabbooking.model.Driver;
import com.crunchycode.cabbooking.model.Ride;
import com.crunchycode.cabbooking.model.RideStatus;
import com.crunchycode.cabbooking.model.VehicleType;
import com.crunchycode.cabbooking.repository.DriverRepository;
import com.crunchycode.cabbooking.repository.RideRepository;
import com.crunchycode.cabbooking.repository.VehicleTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private RideRepository rideRepository;
    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @InjectMocks
    private DriverService driverService;

    private static Driver driver(Long id, String name) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setName(name);
        driver.setEmail(name.toLowerCase().replace(" ", ".") + "@example.com");
        driver.setPhone("9876543210");
        driver.setLicenseNumber("LIC-" + id);
        driver.setAvailable(true);
        return driver;
    }

    private static DriverRequest request(String email, String license, Boolean available) {
        DriverRequest request = new DriverRequest();
        request.setName("New Name");
        request.setEmail(email);
        request.setPhone("9876500000");
        request.setLicenseNumber(license);
        request.setAvailable(available);
        return request;
    }

    // ---------------- create ----------------

    @Test
    void createDriver_savesDriverAndIsAvailableByDefault() {
        when(driverRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(driverRepository.existsByLicenseNumberIgnoreCase("LIC-NEW")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        DriverResponse response = driverService.createDriver(request("new@example.com", "LIC-NEW", null));

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void createDriver_duplicateEmail_throwsConflictAndSavesNothing() {
        when(driverRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> driverService.createDriver(request("taken@example.com", "LIC-1", null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("taken@example.com");
        verify(driverRepository, never()).save(any());
    }

    @Test
    void createDriver_duplicateLicense_throwsConflict() {
        when(driverRepository.existsByEmailIgnoreCase("free@example.com")).thenReturn(false);
        when(driverRepository.existsByLicenseNumberIgnoreCase("LIC-TAKEN")).thenReturn(true);

        assertThatThrownBy(() -> driverService.createDriver(request("free@example.com", "LIC-TAKEN", null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("LIC-TAKEN");
    }

    // ---------------- read ----------------

    @Test
    void getDriverById_returnsDriver() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ravi Kumar")));

        assertThat(driverService.getDriverById(1L).getName()).isEqualTo("Ravi Kumar");
    }

    @Test
    void getDriverById_missing_throwsNotFound() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getDriverById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllDrivers_returnsPageInformation() {
        Pageable pageable = PageRequest.of(0, 2);
        when(driverRepository.search(true, pageable))
                .thenReturn(new PageImpl<>(List.of(driver(1L, "Ravi Kumar"), driver(2L, "Anita Devi")), pageable, 5));

        PageResponse<DriverResponse> result = driverService.getAllDrivers(true, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

    // ---------------- update ----------------

    @Test
    void updateDriver_changesTheFields() {
        Driver existing = driver(1L, "Ravi Kumar");
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(driverRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot("LIC-NEW", 1L)).thenReturn(false);

        DriverResponse response = driverService.updateDriver(1L, request("new@example.com", "LIC-NEW", false));

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void updateDriver_markAvailableWhileOnAcceptedRide_throwsConflict() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ravi Kumar")));
        when(driverRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot("LIC-NEW", 1L)).thenReturn(false);
        when(rideRepository.existsByDriverIdAndStatus(1L, RideStatus.ACCEPTED)).thenReturn(true);

        assertThatThrownBy(() -> driverService.updateDriver(1L, request("new@example.com", "LIC-NEW", true)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("accepted ride");
    }

    @Test
    void updateDriver_missing_throwsNotFound() {
        when(driverRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.updateDriver(5L, request("a@example.com", "LIC", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- delete ----------------

    @Test
    void deleteDriver_withRides_throwsConflict() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ravi Kumar")));
        when(rideRepository.existsByDriverId(1L)).thenReturn(true);

        assertThatThrownBy(() -> driverService.deleteDriver(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("associated rides");
        verify(driverRepository, never()).delete(any());
    }

    @Test
    void deleteDriver_withoutRides_deletesIt() {
        Driver existing = driver(1L, "Ravi Kumar");
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(rideRepository.existsByDriverId(1L)).thenReturn(false);

        driverService.deleteDriver(1L);

        verify(driverRepository).delete(existing);
    }

    @Test
    void deleteDriver_missing_throwsNotFound() {
        when(driverRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.deleteDriver(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- many-to-many ----------------

    @Test
    void assignVehicleType_addsItToTheDriver() {
        Driver existing = driver(1L, "Ravi Kumar");
        VehicleType suv = new VehicleType();
        suv.setId(2L);
        suv.setName("SUV");
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleTypeRepository.findById(2L)).thenReturn(Optional.of(suv));

        driverService.assignVehicleType(1L, 2L);
        driverService.assignVehicleType(1L, 2L); // repeating is safe

        assertThat(existing.getVehicleTypes()).containsExactly(suv);
    }

    @Test
    void assignVehicleType_missingVehicleType_throwsNotFound() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ravi Kumar")));
        when(vehicleTypeRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.assignVehicleType(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle type");
    }

    @Test
    void removeVehicleType_removesTheLink() {
        Driver existing = driver(1L, "Ravi Kumar");
        VehicleType suv = new VehicleType();
        suv.setId(2L);
        existing.getVehicleTypes().add(suv);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleTypeRepository.findById(2L)).thenReturn(Optional.of(suv));

        driverService.removeVehicleType(1L, 2L);

        assertThat(existing.getVehicleTypes()).isEmpty();
    }

    @Test
    void removeVehicleType_notAssigned_throwsNotFound() {
        VehicleType suv = new VehicleType();
        suv.setId(2L);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ravi Kumar")));
        when(vehicleTypeRepository.findById(2L)).thenReturn(Optional.of(suv));

        assertThatThrownBy(() -> driverService.removeVehicleType(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not have");
    }

    @Test
    void getVehicleTypesOfDriver_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleType suv = new VehicleType();
        suv.setId(2L);
        suv.setName("SUV");
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver(1L, "Ravi Kumar")));
        when(vehicleTypeRepository.findByDriversId(1L, pageable)).thenReturn(new PageImpl<>(List.of(suv), pageable, 1));

        PageResponse<VehicleTypeResponse> result = driverService.getVehicleTypesOfDriver(1L, pageable);

        assertThat(result.getContent()).extracting(VehicleTypeResponse::getName).containsExactly("SUV");
    }

    @Test
    void getVehicleTypesOfDriver_missingDriver_throwsNotFound() {
        when(driverRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getVehicleTypesOfDriver(1L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- N+1 demonstration: naive vs optimized ----------------

    private static Ride ride(Long id, Driver driver) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerName("Passenger " + id);
        ride.setPickupLocation("A");
        ride.setDropLocation("B");
        ride.setFare(new BigDecimal("100.00"));
        ride.setStatus(RideStatus.COMPLETED);
        driver.addRide(ride);
        return ride;
    }

    @Test
    void getDriversWithRidesOptimized_loadsRidesInOneExtraQueryAndKeepsPageOrder() {
        Pageable pageable = PageRequest.of(0, 10);
        Driver first = driver(1L, "Ravi Kumar");
        Driver second = driver(2L, "Anita Devi");
        ride(10L, first);
        ride(11L, first);
        ride(12L, second);

        when(driverRepository.search(null, pageable))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        // the fetch query returns the drivers in a different order on purpose
        when(driverRepository.findAllWithRidesByIdIn(List.of(1L, 2L))).thenReturn(List.of(second, first));

        PageResponse<DriverWithRidesResponse> result = driverService.getDriversWithRidesOptimized(pageable);

        assertThat(result.getContent()).extracting(DriverWithRidesResponse::getId).containsExactly(1L, 2L);
        assertThat(result.getContent().get(0).getRides()).hasSize(2);
        assertThat(result.getContent().get(1).getRides()).hasSize(1);
        verify(driverRepository).findAllWithRidesByIdIn(List.of(1L, 2L));
    }

    @Test
    void getDriversWithRidesOptimized_emptyPage_doesNotRunTheFetchQuery() {
        Pageable pageable = PageRequest.of(5, 10);
        when(driverRepository.search(null, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<DriverWithRidesResponse> result = driverService.getDriversWithRidesOptimized(pageable);

        assertThat(result.getContent()).isEmpty();
        verify(driverRepository, never()).findAllWithRidesByIdIn(any());
    }

    @Test
    void getDriversWithRidesNaive_doesNotUseTheFetchQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Driver only = driver(1L, "Ravi Kumar");
        ride(10L, only);
        when(driverRepository.search(null, pageable)).thenReturn(new PageImpl<>(List.of(only), pageable, 1));

        PageResponse<DriverWithRidesResponse> result = driverService.getDriversWithRidesNaive(pageable);

        assertThat(result.getContent().get(0).getRides()).hasSize(1);
        verify(driverRepository, never()).findAllWithRidesByIdIn(any());
    }
}
