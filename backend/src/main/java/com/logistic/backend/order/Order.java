package com.logistic.backend.order;

import com.logistic.backend.catalog.Customer;
import com.logistic.backend.catalog.Driver;
import com.logistic.backend.catalog.Performer;
import com.logistic.backend.catalog.Vehicle;
import com.logistic.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performer_id")
    private Performer performer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "loading_place", nullable = false, columnDefinition = "TEXT")
    private String loadingPlace;

    @Column(name = "loading_contact", columnDefinition = "TEXT")
    private String loadingContact;

    @Column(name = "unloading_place", nullable = false, columnDefinition = "TEXT")
    private String unloadingPlace;

    @Column(name = "unloading_contact", columnDefinition = "TEXT")
    private String unloadingContact;

    @Column(name = "trip_count", nullable = false)
    private int tripCount = 1;

    @Column(name = "price_per_trip", precision = 10, scale = 2)
    private BigDecimal pricePerTrip;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "template_version", nullable = false)
    private int templateVersion = 1;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;
}
