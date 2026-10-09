package com.traveleasy.bus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bus_operators", indexes = {
        @Index(name = "idx_operator_code", columnList = "operator_code", unique = true),
        @Index(name = "idx_operator_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusOperator extends BaseEntity {

    @Column(name = "operator_code", nullable = false, unique = true, length = 50)
    private String operatorCode;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 25)
    private String contactPhone;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "operator", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bus> buses = new ArrayList<>();

    @OneToMany(mappedBy = "operator", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Route> routes = new ArrayList<>();
}
