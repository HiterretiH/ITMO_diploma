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
        name = "customer_route_hints",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_route_hint_customer_kind_place",
                        columnNames = {"customer_id", "kind", "place_key"}))
@Getter
@Setter
public class CustomerRouteHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RouteHintKind kind;

    @Column(name = "place_key", nullable = false, length = 64)
    private String placeKey;

    @Column(name = "place_text", nullable = false, columnDefinition = "TEXT")
    private String placeText;

    @Column(name = "contact_text", columnDefinition = "TEXT")
    private String contactText;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
