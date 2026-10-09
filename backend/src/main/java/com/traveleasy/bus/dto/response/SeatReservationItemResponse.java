package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservationItemResponse {
    private String seatNumber;
    private Deck deck;
    private SeatType seatType;
    private SeatInventoryStatus status;
    private String bookingReference;
    private String lockedBySession;
    private LocalDateTime lockExpiryTime;
    private BigDecimal seatFare;
}
