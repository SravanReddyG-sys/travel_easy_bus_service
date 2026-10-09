package com.traveleasy.bus.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SeatBookedEvent extends BusDomainEvent {
    private UUID scheduleId;
    private String bookingReference;
    private List<String> seatNumbers;
    private BigDecimal totalAmount;
    private String currency;
}
