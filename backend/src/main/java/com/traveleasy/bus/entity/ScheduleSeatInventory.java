package com.traveleasy.bus.entity;

import com.traveleasy.bus.enums.SeatInventoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_seat_inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_schedule_seat_inventory", columnNames = {"schedule_id", "seat_id"})
        },
        indexes = {
                @Index(name = "idx_sched_inv_schedule_id", columnList = "schedule_id"),
                @Index(name = "idx_sched_inv_status", columnList = "schedule_id, status"),
                @Index(name = "idx_sched_inv_expiry", columnList = "status, lock_expiry_time"),
                @Index(name = "idx_sched_inv_booking_ref", columnList = "booking_reference")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSeatInventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_schedule"))
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_seat"))
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SeatInventoryStatus status = SeatInventoryStatus.AVAILABLE;

    @Column(name = "lock_expiry_time")
    private LocalDateTime lockExpiryTime;

    @Column(name = "locked_by_booking_session", length = 100)
    private String lockedByBookingSession;

    @Column(name = "booking_reference", length = 100)
    private String bookingReference;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
