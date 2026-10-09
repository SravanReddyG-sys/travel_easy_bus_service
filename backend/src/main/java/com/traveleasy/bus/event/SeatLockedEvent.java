package com.traveleasy.bus.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLockedEvent extends BusDomainEvent {
    private UUID scheduleId;
    private List<String> seatNumbers;
    private String lockToken;
    private LocalDateTime lockExpiryTime;
}
