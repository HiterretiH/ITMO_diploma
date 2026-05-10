package com.logistic.backend.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select coalesce(max(o.orderNumber), 0) from Order o where o.owner.id = :ownerId")
    int maxOrderNumberForOwner(@Param("ownerId") Long ownerId);

    @Query(
            """
            select distinct o from Order o
            join fetch o.owner
            join fetch o.customer
            join fetch o.performer
            left join fetch o.vehicle
            left join fetch o.driver
            where o.id = :id
            """)
    Optional<Order> findDetailedById(@Param("id") Long id);

    @Query(
            """
            select distinct o from Order o
            join fetch o.owner
            join fetch o.customer
            join fetch o.performer
            left join fetch o.vehicle
            left join fetch o.driver
            where o.id = :id and o.owner.id = :ownerId
            """)
    Optional<Order> findDetailedByIdAndOwner_Id(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Query(
            """
            select distinct o from Order o
            join fetch o.owner
            join fetch o.customer
            join fetch o.performer
            left join fetch o.vehicle
            left join fetch o.driver
            order by o.orderDate desc, o.id desc
            """)
    List<Order> findAllDetailedOrderByOrderDateDesc();

    @Query(
            """
            select distinct o from Order o
            join fetch o.owner
            join fetch o.customer
            join fetch o.performer
            left join fetch o.vehicle
            left join fetch o.driver
            where o.owner.id = :ownerId
            order by o.orderDate desc, o.id desc
            """)
    List<Order> findAllDetailedByOwner_IdOrderByOrderDateDesc(@Param("ownerId") Long ownerId);
}
