package com.crunchycode.cabbooking.repository;

import com.crunchycode.cabbooking.model.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Many-to-many: vehicle types of a given driver
    Page<VehicleType> findByDriversId(Long driverId, Pageable pageable);
}
