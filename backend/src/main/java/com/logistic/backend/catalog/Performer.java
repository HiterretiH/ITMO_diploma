package com.logistic.backend.catalog;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "performers")
@Getter
@Setter
public class Performer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "short_name", nullable = false, columnDefinition = "TEXT")
    private String shortName;

    @Column(name = "full_name", columnDefinition = "TEXT")
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String phone;

    @Column(name = "bank_name", columnDefinition = "TEXT")
    private String bankName;

    @Column(columnDefinition = "TEXT")
    private String inn;

    @Column(columnDefinition = "TEXT")
    private String bik;

    @Column(columnDefinition = "TEXT")
    private String kpp;

    @Column(name = "payment_account", columnDefinition = "TEXT")
    private String paymentAccount;

    @Column(name = "corr_account", columnDefinition = "TEXT")
    private String corrAccount;

    @Column(columnDefinition = "TEXT")
    private String requisites;
}
