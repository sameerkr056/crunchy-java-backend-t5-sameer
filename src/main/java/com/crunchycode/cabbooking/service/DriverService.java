package com.crunchycode.cabbooking.service;

import com.crunchycode.cabbooking.dto.request.DriverRequest;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.DriverWithRidesResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.exception.ResourceNotFoundException;
import com.crunchycode.cabbooking.model.Driver;
import com.crunchycode.cabbooking.model.RideStatus;
import com.crunchycode.cabbooking.model.VehicleType;
import com.crunchycode.cabbooking.repository.DriverRepository;
import com.crunchycode.cabbooking.repository.RideRepository;
import com.crunchycode.cabbooking.repository.VehicleTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only transactions by default. Methods that change data have their own @Transactional.
 * The transaction (and the Hibernate session) stays open until the DTOs are built,
 * so lazy loading works here and nowhere else.
 */
@Service
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public DriverService(DriverRepository driverRepository,
                         RideRepository rideRepository,
                         VehicleTypeRepository vehicleTypeRepository) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    @Transactional
    public DriverResponse createDriver(DriverRequest request) {
        checkEmailAndLicenseAreFree(request, null);

        Driver driver = new Driver();
        driver.setName(request.getName());
        driver.setEmail(request.getEmail());
        driver.setPhone(request.getPhone());
        driver.setLicenseNumber(request.getLicenseNumber());
        // A new driver is available unless the client says otherwise
        driver.setAvailable(request.getAvailable() == null || request.getAvailable());
        return DriverResponse.from(driverRepository.save(driver));
    }

    public PageResponse<DriverResponse> getAllDrivers(Boolean available, Pageable pageable) {
        Page<DriverResponse> page = driverRepository.search(available, pageable).map(DriverResponse::from);
        return PageResponse.from(page);
    }

    public DriverResponse getDriverById(Long id) {
        return DriverResponse.from(findDriverOrThrow(id));
    }

    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest request) {
        Driver driver = findDriverOrThrow(id);
        checkEmailAndLicenseAreFree(request, id);

        // A driver on an ACCEPTED ride cannot be marked available by hand
        if (Boolean.TRUE.equals(request.getAvailable())
                && rideRepository.existsByDriverIdAndStatus(id, RideStatus.ACCEPTED)) {
            throw new ResourceConflictException(
                    "Driver with id " + id + " is on an accepted ride and cannot be marked available");
        }

        driver.setName(request.getName());
        driver.setEmail(request.getEmail());
        driver.setPhone(request.getPhone());
        driver.setLicenseNumber(request.getLicenseNumber());
        if (request.getAvailable() != null) {
            driver.setAvailable(request.getAvailable());
        }
        // No save() needed: the driver is managed, Hibernate saves the changes when the transaction commits
        return DriverResponse.from(driver);
    }

    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = findDriverOrThrow(id);
        if (rideRepository.existsByDriverId(id)) {
            throw new ResourceConflictException(
                    "Driver with id " + id + " cannot be deleted because the driver has associated rides");
        }
        // Also removes this driver's rows from the driver_vehicle_types join table
        driverRepository.delete(driver);
    }

    // ------------------------------------------------------------------
    // Many-to-many: Driver <-> VehicleType
    // ------------------------------------------------------------------

    @Transactional
    public void assignVehicleType(Long driverId, Long vehicleTypeId) {
        Driver driver = findDriverOrThrow(driverId);
        VehicleType vehicleType = findVehicleTypeOrThrow(vehicleTypeId);
        // Set.add does nothing if the pair already exists, so this call is safe to repeat
        driver.getVehicleTypes().add(vehicleType);
    }

    @Transactional
    public void removeVehicleType(Long driverId, Long vehicleTypeId) {
        Driver driver = findDriverOrThrow(driverId);
        VehicleType vehicleType = findVehicleTypeOrThrow(vehicleTypeId);
        boolean removed = driver.getVehicleTypes().remove(vehicleType);
        if (!removed) {
            throw new ResourceNotFoundException(
                    "Driver " + driverId + " does not have vehicle type " + vehicleTypeId);
        }
    }

    public PageResponse<VehicleTypeResponse> getVehicleTypesOfDriver(Long driverId, Pageable pageable) {
        findDriverOrThrow(driverId);
        Page<VehicleTypeResponse> page = vehicleTypeRepository.findByDriversId(driverId, pageable)
                .map(VehicleTypeResponse::from);
        return PageResponse.from(page);
    }

    // ------------------------------------------------------------------
    // N+1 demonstration: drivers together with their rides
    // ------------------------------------------------------------------

    /**
     * BEFORE: this version has the N+1 problem.
     * Query 1 loads one page of drivers (plus a count query for the page info).
     * Then DriverWithRidesResponse.from() calls driver.getRides() for every driver,
     * and Hibernate runs ONE extra "select ... from rides where driver_id=?" per driver.
     * For a page of N drivers that is 1 + N queries (+ 1 count).
     */
    public PageResponse<DriverWithRidesResponse> getDriversWithRidesNaive(Pageable pageable) {
        Page<Driver> drivers = driverRepository.search(null, pageable);
        return PageResponse.from(drivers.map(DriverWithRidesResponse::from));
    }

    /**
     * AFTER: two-step loading with JOIN FETCH.
     * Step 1: load one page of drivers (normal pagination, done in the database).
     * Step 2: load the rides of exactly those drivers with ONE "left join fetch" query.
     * Total: always 2 queries (+ 1 count), no matter how many drivers are on the page.
     *
     * Why two steps? JOIN FETCH on a collection together with Pageable would make Hibernate
     * load everything and paginate in memory (warning HHH90003004). Paging the drivers first
     * and fetching the rides afterwards keeps the pagination correct and fast.
     */
    public PageResponse<DriverWithRidesResponse> getDriversWithRidesOptimized(Pageable pageable) {
        Page<Driver> page = driverRepository.search(null, pageable);

        List<Long> driverIds = page.getContent().stream().map(Driver::getId).toList();
        Map<Long, Driver> driversWithRides = new HashMap<>();
        if (!driverIds.isEmpty()) {
            for (Driver driver : driverRepository.findAllWithRidesByIdIn(driverIds)) {
                driversWithRides.put(driver.getId(), driver);
            }
        }

        // Keep the order of the first query (it contains the requested sorting)
        return PageResponse.from(page.map(driver ->
                DriverWithRidesResponse.from(driversWithRides.get(driver.getId()))));
    }

    // ------------------------------------------------------------------

    private void checkEmailAndLicenseAreFree(DriverRequest request, Long currentDriverId) {
        boolean emailTaken = currentDriverId == null
                ? driverRepository.existsByEmailIgnoreCase(request.getEmail())
                : driverRepository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), currentDriverId);
        if (emailTaken) {
            throw new ResourceConflictException("A driver with email " + request.getEmail() + " already exists");
        }

        boolean licenseTaken = currentDriverId == null
                ? driverRepository.existsByLicenseNumberIgnoreCase(request.getLicenseNumber())
                : driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(request.getLicenseNumber(), currentDriverId);
        if (licenseTaken) {
            throw new ResourceConflictException(
                    "A driver with license number " + request.getLicenseNumber() + " already exists");
        }
    }

    private Driver findDriverOrThrow(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }

    private VehicleType findVehicleTypeOrThrow(Long id) {
        return vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with id: " + id));
    }
}
