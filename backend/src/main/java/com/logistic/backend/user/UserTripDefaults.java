package com.logistic.backend.user;

import com.logistic.backend.catalog.Driver;
import com.logistic.backend.catalog.Performer;
import com.logistic.backend.catalog.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_trip_defaults")
@Getter
@Setter
public class UserTripDefaults {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_performer_id")
    private Performer lastPerformer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_driver_id")
    private Driver lastDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_vehicle_id")
    private Vehicle lastVehicle;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
