package com.traveleasy.bus.event;

import com.traveleasy.bus.entity.BusEventOutbox;

public interface DomainEventPublisher {
    void publish(BusDomainEvent event, BusEventOutbox outboxRecord);
}
