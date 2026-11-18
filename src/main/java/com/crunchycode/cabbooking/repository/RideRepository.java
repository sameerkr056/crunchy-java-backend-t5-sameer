package com.crunchycode.cabbooking.repository;

import com.crunchycode.cabbooking.model.Ride;
import com.crunchycode.cabbooking.model.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    // Optional filters: a null parameter means "no filter".
    // The entity graph loads the driver in the same query, so listing rides never triggers extra queries.
    @EntityGraph(attributePaths = "driver")
    @Query("select r from Ride r where (:status is null or r.status = :status) "
            + "and (:driverId is null or r.driver.id = :driverId)")
    Page<Ride> search(@Param("status") RideStatus status,
                      @Param("driverId") Long driverId,
                      Pageable pageable);

    boolean existsByDriverId(Long driverId);

    boolean existsByDriverIdAndStatus(Long driverId, RideStatus status);
}
