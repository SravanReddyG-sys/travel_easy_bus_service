package com.traveleasy.bus.entity;

import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bus_seat_number", columnNames = {"bus_id", "seat_number"})
        },
        indexes = {
                @Index(name = "idx_seats_bus_id", columnList = "bus_id"),
                @Index(name = "idx_seats_deck", columnList = "deck"),
                @Index(name = "idx_seats_type", columnList = "seat_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_bus"))
    private Bus bus;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "deck", nullable = false, length = 10)
    @Builder.Default
    private Deck deck = Deck.LOWER;

    @Column(name = "row_num", nullable = false)
    private Integer rowNum;

    @Column(name = "col_num", nullable = false)
    private Integer colNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    @Builder.Default
    private SeatType seatType = SeatType.SEATER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
