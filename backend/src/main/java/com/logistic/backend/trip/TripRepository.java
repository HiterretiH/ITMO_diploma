package com.logistic.backend.trip;

import com.logistic.backend.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("select t from Trip t join fetch t.owner where t.owner = :owner order by t.updatedAt desc")
    List<Trip> findByOwnerOrderByUpdatedAtDesc(@Param("owner") User owner);

    @Query(
            "select t from Trip t join fetch t.owner where t.owner = :owner and t.status = :status order by"
                    + " t.updatedAt desc")
    List<Trip> findByOwnerAndStatusOrderByUpdatedAtDesc(
            @Param("owner") User owner, @Param("status") TripStatus status);

    Optional<Trip> findByIdAndOwner(Long id, User owner);

    @Query(
            """
            select distinct t from Trip t
            join fetch t.owner
            left join fetch t.shipper
            left join fetch t.consignee
            left join fetch t.driver
            left join fetch t.vehicle
            where t.id = :id and t.owner = :owner
            """)
    Optional<Trip> findDetailedForOwner(@Param("id") Long id, @Param("owner") User owner);

    @Query("select t from Trip t join fetch t.owner order by t.updatedAt desc")
    List<Trip> findAllByOrderByUpdatedAtDesc();

    @Query("select t from Trip t join fetch t.owner where t.status = :status order by t.updatedAt desc")
    List<Trip> findByStatusOrderByUpdatedAtDesc(@Param("status") TripStatus status);
}
