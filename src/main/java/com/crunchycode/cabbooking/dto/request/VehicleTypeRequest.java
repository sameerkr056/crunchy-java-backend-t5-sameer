package com.crunchycode.cabbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VehicleTypeRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must be at most 50 characters")
    @Schema(description = "Unique name", example = "Electric Car")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    @Schema(description = "Optional description", example = "Zero emission car")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
