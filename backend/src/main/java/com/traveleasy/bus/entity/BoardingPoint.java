package com.traveleasy.bus.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "boarding_points",
        indexes = {
                @Index(name = "idx_boarding_route_id", columnList = "route_id"),
                @Index(name = "idx_boarding_stop_id", columnList = "stop_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardingPoint extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_boarding_route"))
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", foreignKey = @ForeignKey(name = "fk_boarding_stop"))
    private RouteStop stop;

    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    @Column(name = "landmark", length = 200)
    private String landmark;

    @Column(name = "departure_offset_minutes", nullable = false)
    @Builder.Default
    private Integer departureOffsetMinutes = 0;

    @Column(name = "contact_phone", length = 25)
    private String contactPhone;
}
