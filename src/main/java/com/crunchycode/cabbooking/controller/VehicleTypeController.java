package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.crunchycode.cabbooking.dto.request.PageParams;
import com.crunchycode.cabbooking.dto.request.VehicleTypeRequest;
import com.crunchycode.cabbooking.dto.response.DriverResponse;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.VehicleTypeResponse;
import com.crunchycode.cabbooking.service.VehicleTypeService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@Tag(name = "Vehicle types", description = "Manage vehicle types (Sedan, SUV, ...). Reading needs any logged-in user, changes need ADMIN.")
@RestController
@RequestMapping("/api/v1/vehicle-types")
@Validated
public class VehicleTypeController {

    private static final Set<String> VEHICLE_TYPE_SORT_FIELDS = Set.of("id", "name", "description");
    private static final Set<String> DRIVER_SORT_FIELDS =
            Set.of("id", "name", "email", "phone", "licenseNumber", "available");

    private final VehicleTypeService vehicleTypeService;

    public VehicleTypeController(VehicleTypeService vehicleTypeService) {
        this.vehicleTypeService = vehicleTypeService;
    }

    @Operation(summary = "Create a vehicle type",
            description = "**ADMIN only.** The name must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleTypeResponse> createVehicleType(@Valid @RequestBody VehicleTypeRequest request) {
        VehicleTypeResponse created = vehicleTypeService.createVehicleType(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // GET /api/v1/vehicle-types?page=0&size=10&sortBy=id&direction=asc
    @Operation(summary = "List vehicle types (paginated)",
            description = "**USER or ADMIN.** Sortable fields: id, name, description.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameter (page, size, sortBy or direction)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageResponse<VehicleTypeResponse>> getAllVehicleTypes(@ParameterObject @Valid PageParams pageParams) {
        Pageable pageable = pageParams.toPageable(VEHICLE_TYPE_SORT_FIELDS);
        return ResponseEntity.ok(vehicleTypeService.getAllVehicleTypes(pageable));
    }

    @Operation(summary = "Get one vehicle type",
            description = "**USER or ADMIN.**")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<VehicleTypeResponse> getVehicleTypeById(
            @PathVariable("id") @Positive(message = "Vehicle type id must be a positive number") Long id) {
        return ResponseEntity.ok(vehicleTypeService.getVehicleTypeById(id));
    }

    @Operation(summary = "Update a vehicle type",
            description = "**ADMIN only.**")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleTypeResponse> updateVehicleType(
            @PathVariable("id") @Positive(message = "Vehicle type id must be a positive number") Long id,
            @Valid @RequestBody VehicleTypeRequest request) {
        return ResponseEntity.ok(vehicleTypeService.updateVehicleType(id, request));
    }

    @Operation(summary = "Delete a vehicle type",
            description = "**ADMIN only.** A vehicle type that drivers still use cannot be deleted (409).")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVehicleType(
            @PathVariable("id") @Positive(message = "Vehicle type id must be a positive number") Long id) {
        vehicleTypeService.deleteVehicleType(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/vehicle-types/{id}/drivers?page=0&size=10  (drivers that have this vehicle type)
    @Operation(summary = "List the drivers that have this vehicle type (paginated)",
            description = "**USER or ADMIN.** The other side of the many-to-many relationship.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameter (page, size, sortBy or direction)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @GetMapping("/{id}/drivers")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageResponse<DriverResponse>> getDriversOfVehicleType(
            @PathVariable("id") @Positive(message = "Vehicle type id must be a positive number") Long id,
            @ParameterObject @Valid PageParams pageParams) {
        Pageable pageable = pageParams.toPageable(DRIVER_SORT_FIELDS);
        return ResponseEntity.ok(vehicleTypeService.getDriversOfVehicleType(id, pageable));
    }
}
