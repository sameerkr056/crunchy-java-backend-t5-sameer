package com.crunchycode.cabbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(nullable = false)
    private boolean available;

    /**
     * ONE-TO-MANY (inverse side). The foreign key lives in rides.driver_id, so Ride is the owning side.
     * LAZY (the default for collections): rides are only loaded when somebody asks for them.
     * That is exactly why looping over drivers and calling getRides() causes the N+1 problem.
     */
    @OneToMany(mappedBy = "driver")
    @OrderBy("id ASC")
    private List<Ride> rides = new ArrayList<>();

    /**
     * MANY-TO-MANY (owning side). Driver owns the relationship because a driver "chooses" its vehicle types.
     * The join table driver_vehicle_types is written from this side only.
     */
    @ManyToMany
    @JoinTable(
            name = "driver_vehicle_types",
            joinColumns = @JoinColumn(name = "driver_id"),
            inverseJoinColumns = @JoinColumn(name = "vehicle_type_id"))
    private Set<VehicleType> vehicleTypes = new HashSet<>();

    public Driver() {
    }

    // Helper methods keep BOTH sides of the one-to-many relationship in sync.
    public void addRide(Ride ride) {
        rides.add(ride);
        ride.setDriver(this);
    }

    public void removeRide(Ride ride) {
        rides.remove(ride);
        ride.setDriver(null);
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<Ride> getRides() {
        return rides;
    }

    public Set<VehicleType> getVehicleTypes() {
        return vehicleTypes;
    }
}
