
package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.dto.request.DriverRequest;
import com.crunchycode.cabbooking.dto.request.PageParams;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.DriverWithRidesResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.service.DriverService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/drivers")
@Validated
public class DriverController {

    private static final Set<String> DRIVER_SORT_FIELDS =
            Set.of("id", "name", "email", "phone", "licenseNumber", "available");

    private static final Set<String> VEHICLE_TYPE_SORT_FIELDS =
            Set.of("id", "name", "description");

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody DriverRequest request) {

        DriverResponse created = driverService.createDriver(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageResponse<DriverResponse>> getAllDrivers(
            @RequestParam(name = "available", required = false) Boolean available,
            @Valid PageParams pageParams) {

        Pageable pageable = pageParams.toPageable(DRIVER_SORT_FIELDS);

        return ResponseEntity.ok(
                driverService.getAllDrivers(available, pageable)
        );
    }

    /**
     * N+1 demonstration endpoint.
     *
     * mode=naive      -> demonstrates N+1 queries
     * mode=optimized  -> optimized query approach (default)
     */
    @GetMapping("/with-rides")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageResponse<DriverWithRidesResponse>>
    getDriversWithRides(
            @RequestParam(
                    name = "mode",
                    defaultValue = "optimized"
            )
            @Pattern(
                    regexp = "naive|optimized",
                    message = "mode must be naive or optimized"
            )
            String mode,
            @Valid PageParams pageParams) {

        Pageable pageable = pageParams.toPageable(DRIVER_SORT_FIELDS);

        if ("naive".equals(mode)) {
            return ResponseEntity.ok(
                    driverService.getDriversWithRidesNaive(pageable)
            );
        }

        return ResponseEntity.ok(
                driverService.getDriversWithRidesOptimized(pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable("id")
            @Positive(message = "Driver id must be a positive number")
            Long id) {

        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable("id")
            @Positive(message = "Driver id must be a positive number")
            Long id,
            @Valid @RequestBody DriverRequest request) {

        return ResponseEntity.ok(
                driverService.updateDriver(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable("id")
            @Positive(message = "Driver id must be a positive number")
            Long id) {

        driverService.deleteDriver(id);

        return ResponseEntity.noContent().build();
    }

    // Driver and VehicleType relationship

    @GetMapping("/{id}/vehicle-types")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageResponse<VehicleTypeResponse>>
    getVehicleTypesOfDriver(
            @PathVariable("id")
            @Positive(message = "Driver id must be a positive number")
            Long id,
            @Valid PageParams pageParams) {

        Pageable pageable = pageParams.toPageable(
                VEHICLE_TYPE_SORT_FIELDS
        );

        return ResponseEntity.ok(
                driverService.getVehicleTypesOfDriver(id, pageable)
        );
    }

    @PutMapping("/{id}/vehicle-types/{vehicleTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignVehicleType(
            @PathVariable("id")
            @Positive(message = "Driver id must be a positive number")
            Long id,
            @PathVariable("vehicleTypeId")
            @Positive(message = "Vehicle type id must be a positive number")
            Long vehicleTypeId) {

        driverService.assignVehicleType(id, vehicleTypeId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/vehicle-types/{vehicleTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeVehicleType(
            @PathVariable("id")
            @Positive(message = "Driver id must be a positive number")
            Long id,
            @PathVariable("vehicleTypeId")
            @Positive(message = "Vehicle type id must be a positive number")
            Long vehicleTypeId) {

        driverService.removeVehicleType(id, vehicleTypeId);

        return ResponseEntity.noContent().build();
    }
}
