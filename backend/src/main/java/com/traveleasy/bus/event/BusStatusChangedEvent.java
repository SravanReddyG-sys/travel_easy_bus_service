package com.traveleasy.bus.event;

import com.traveleasy.bus.enums.BusStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BusStatusChangedEvent extends BusDomainEvent {
    private UUID busId;
    private String registrationNumber;
    private BusStatus status;
}
