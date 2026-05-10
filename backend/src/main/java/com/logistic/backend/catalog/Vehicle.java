package com.logistic.backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private Performer owner;

    @Column(name = "brand_model", columnDefinition = "TEXT")
    private String brandModel;

    @Column(name = "plate_number", columnDefinition = "TEXT")
    private String plateNumber;

    @Column(columnDefinition = "TEXT")
    private String type;

    @Column(name = "is_default", nullable = false)
    private boolean defaultForPerformer = false;
}
