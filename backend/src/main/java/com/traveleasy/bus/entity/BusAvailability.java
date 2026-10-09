package com.traveleasy.bus.entity;

import com.traveleasy.bus.enums.AvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_availability",
        indexes = {
                @Index(name = "idx_bus_avail_schedule", columnList = "schedule_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusAvailability extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avail_schedule"))
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AvailabilityStatus status;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "effective_from", nullable = false)
    @Builder.Default
    private LocalDateTime effectiveFrom = LocalDateTime.now();
}
