package com.traveleasy.bus.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_stops",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_route_stop_order", columnNames = {"route_id", "stop_order"})
        },
        indexes = {
                @Index(name = "idx_route_stops_route_id", columnList = "route_id"),
                @Index(name = "idx_route_stops_order", columnList = "route_id, stop_order")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStop extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_stop_route"))
    private Route route;

    @Column(name = "stop_name", nullable = false, length = 100)
    private String stopName;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "distance_from_origin_km", nullable = false)
    private Double distanceFromOriginKm;
}
