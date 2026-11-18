package com.crunchycode.cabbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.crunchycode.cabbooking.model.VehicleType;

public class VehicleTypeResponse {

    @Schema(example = "2")
    private final Long id;
    @Schema(example = "Sedan")
    private final String name;
    private final String description;

    public VehicleTypeResponse(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static VehicleTypeResponse from(VehicleType vehicleType) {
        return new VehicleTypeResponse(vehicleType.getId(), vehicleType.getName(), vehicleType.getDescription());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
