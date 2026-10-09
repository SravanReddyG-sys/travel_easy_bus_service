package com.traveleasy.bus.entity;

import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bus_operator_number", columnNames = {"operator_id", "bus_number"}),
                @UniqueConstraint(name = "uk_bus_registration", columnNames = {"registration_number"})
        },
        indexes = {
                @Index(name = "idx_buses_operator_id", columnList = "operator_id"),
                @Index(name = "idx_buses_status", columnList = "status"),
                @Index(name = "idx_buses_type", columnList = "bus_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bus extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bus_operator"))
    private BusOperator operator;

    @Column(name = "bus_number", nullable = false, length = 50)
    private String busNumber;

    @Column(name = "registration_number", nullable = false, length = 50)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "bus_type", nullable = false, length = 30)
    private BusType busType;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BusStatus status = BusStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bus_amenities",
            joinColumns = @JoinColumn(name = "bus_id", foreignKey = @ForeignKey(name = "fk_amenity_bus")))
    @Column(name = "amenity", nullable = false, length = 100)
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Seat> physicalSeats = new ArrayList<>();
}
