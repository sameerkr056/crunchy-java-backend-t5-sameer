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
import com.crunchycode.cabbooking.dto.request.RideRequest;
import com.crunchycode.cabbooking.dto.request.RideStatusRequest;
import com.crunchycode.cabbooking.dto.response.PageResponse;
import com.crunchycode.cabbooking.dto.response.RideResponse;
import com.crunchycode.cabbooking.model.RideStatus;
import com.crunchycode.cabbooking.service.RideService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

@Tag(name = "Rides", description = "Book rides and follow them through REQUESTED, ACCEPTED, COMPLETED or CANCELLED.")
@RestController
@RequestMapping("/api/v1/rides")
@Validated
public class RideController {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "passengerName", "pickupLocation", "dropLocation", "fare", "status", "bookingTime");

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @Operation(summary = "Create a ride",
            description = "**USER or ADMIN.** The ride always starts as REQUESTED (the client cannot choose the status). The driver must exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody RideRequest request) {
        RideResponse created = rideService.createRide(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // GET /api/v1/rides?page=0&size=10&sortBy=id&direction=asc&status=REQUESTED&driverId=1
    @Operation(summary = "List rides (paginated)",
            description = "**USER or ADMIN.** One page of rides. Optional filters `status` and `driverId`. Sortable fields: id, passengerName, pickupLocation, dropLocation, fare, status, bookingTime.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameter (page, size, sortBy or direction)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageResponse<RideResponse>> getAllRides(
            @Parameter(description = "Optional filter by ride status")
            @RequestParam(name = "status", required = false) RideStatus status,
            @RequestParam(name = "driverId", required = false)
            @Positive(message = "Driver id must be a positive number") Long driverId,
            @ParameterObject @Valid PageParams pageParams) {
        Pageable pageable = pageParams.toPageable(SORT_FIELDS);
        return ResponseEntity.ok(rideService.getAllRides(status, driverId, pageable));
    }

    @Operation(summary = "Get one ride",
            description = "**USER or ADMIN.**")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RideResponse> getRideById(
            @PathVariable("id") @Positive(message = "Ride id must be a positive number") Long id) {
        return ResponseEntity.ok(rideService.getRideById(id));
    }

    @Operation(summary = "Update ride details",
            description = "**USER or ADMIN.** Only REQUESTED rides can be edited (409 otherwise). The status cannot be changed here.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RideResponse> updateRide(
            @PathVariable("id") @Positive(message = "Ride id must be a positive number") Long id,
            @Valid @RequestBody RideRequest request) {
        return ResponseEntity.ok(rideService.updateRide(id, request));
    }

    @Operation(summary = "Delete a ride",
            description = "**ADMIN only.** Deleting an ACCEPTED ride makes its driver available again.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRide(
            @PathVariable("id") @Positive(message = "Ride id must be a positive number") Long id) {
        rideService.deleteRide(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Accept a ride",
            description = "**ADMIN only.** REQUESTED to ACCEPTED. The driver must be available and becomes unavailable.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RideResponse> acceptRide(
            @PathVariable("id") @Positive(message = "Ride id must be a positive number") Long id) {
        return ResponseEntity.ok(rideService.acceptRide(id));
    }

    @Operation(summary = "Complete or cancel a ride",
            description = "**ADMIN only.** Only an ACCEPTED ride can become COMPLETED or CANCELLED. The driver becomes available again.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RideResponse> updateRideStatus(
            @PathVariable("id") @Positive(message = "Ride id must be a positive number") Long id,
            @Valid @RequestBody RideStatusRequest request) {
        return ResponseEntity.ok(rideService.updateRideStatus(id, request.getStatus()));
    }
}
