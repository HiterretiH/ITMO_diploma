package com.logistic.backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
