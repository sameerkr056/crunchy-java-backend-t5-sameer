package com.crunchycode.cabbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.crunchycode.cabbooking.model.RideStatus;
import jakarta.validation.constraints.NotNull;

public class RideStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New status. Only COMPLETED or CANCELLED are allowed, and only for ACCEPTED rides", example = "COMPLETED")
    private RideStatus status;

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }
}
