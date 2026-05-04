package com.logistic.backend.trip;

import com.logistic.backend.catalog.Counterparty;
import com.logistic.backend.catalog.Driver;
import com.logistic.backend.catalog.Vehicle;
import com.logistic.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TripStatus status = TripStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id")
    private Counterparty shipper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignee_id")
    private Counterparty consignee;

    @Column(name = "cargo_description", length = 2048)
    private String cargoDescription;

    @Column(name = "cargo_weight_kg", precision = 14, scale = 3)
    private BigDecimal cargoWeightKg;

    @Column(name = "route_from", length = 512)
    private String routeFrom;

    @Column(name = "route_to", length = 512)
    private String routeTo;

    @Column(name = "load_date")
    private LocalDate loadDate;

    @Column(name = "unload_date")
    private LocalDate unloadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "price_amount", precision = 14, scale = 2)
    private BigDecimal priceAmount;

    @Column(length = 8)
    private String currency = "RUB";

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
