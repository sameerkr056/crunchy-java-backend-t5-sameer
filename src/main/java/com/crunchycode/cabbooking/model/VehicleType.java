package com.crunchycode.cabbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicle_types")
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /**
     * MANY-TO-MANY (inverse side). mappedBy = "vehicleTypes" points to Driver.vehicleTypes, the owning side.
     * This side is read-only: to link a driver and a vehicle type, change Driver.vehicleTypes.
     * It is never exposed in the JSON responses.
     */
    @ManyToMany(mappedBy = "vehicleTypes")
    private Set<Driver> drivers = new HashSet<>();

    public VehicleType() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Set<Driver> getDrivers() {
        return drivers;
    }
}
