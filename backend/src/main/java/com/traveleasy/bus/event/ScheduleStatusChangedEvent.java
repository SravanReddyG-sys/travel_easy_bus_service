package com.traveleasy.bus.event;

import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
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
public class ScheduleStatusChangedEvent extends BusDomainEvent {
    private UUID scheduleId;
    private ScheduleStatus status;
    private AvailabilityStatus availabilityStatus;
    private String reason;
}
