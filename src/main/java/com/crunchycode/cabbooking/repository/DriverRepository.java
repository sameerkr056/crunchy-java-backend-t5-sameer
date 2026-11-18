package com.crunchycode.cabbooking.repository;

import com.crunchycode.cabbooking.model.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    // One page of drivers. available = null means "no filter".
    @Query("select d from Driver d where (:available is null or d.available = :available)")
    Page<Driver> search(@Param("available") Boolean available, Pageable pageable);

    // STEP 2 of the N+1 fix: load the rides of the given drivers in ONE query using JOIN FETCH.
    @Query("select d from Driver d left join fetch d.rides where d.id in :ids")
    List<Driver> findAllWithRidesByIdIn(@Param("ids") List<Long> ids);

    // Many-to-many: drivers that have a given vehicle type
    Page<Driver> findByVehicleTypesId(Long vehicleTypeId, Pageable pageable);

    boolean existsByVehicleTypesId(Long vehicleTypeId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, Long id);
}
