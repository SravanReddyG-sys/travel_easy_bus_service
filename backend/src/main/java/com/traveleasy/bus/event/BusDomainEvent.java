package com.traveleasy.bus.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BusDomainEvent implements Serializable {
    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private UUID aggregateId;
    private Instant timestamp;
    private String correlationId;
}
