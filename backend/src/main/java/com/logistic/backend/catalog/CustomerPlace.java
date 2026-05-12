package com.logistic.backend.catalog;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "customer_places",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_customer_place_customer_kind_address",
                        columnNames = {"customer_id", "kind", "address_key"}))
@Getter
@Setter
public class CustomerPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CustomerPlaceKind kind;

    @Column(name = "address_key", nullable = false, length = 64)
    private String addressKey;

    @Column(name = "address_text", nullable = false, columnDefinition = "TEXT")
    private String addressText;

    @Column(name = "contact_text", columnDefinition = "TEXT")
    private String contactText;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
