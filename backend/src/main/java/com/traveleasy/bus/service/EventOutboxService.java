package com.traveleasy.bus.service;

import com.traveleasy.bus.entity.BusEventOutbox;
import com.traveleasy.bus.event.BusDomainEvent;

import java.util.List;
import java.util.UUID;

public interface EventOutboxService {

    BusEventOutbox saveEvent(BusDomainEvent event);

    void processPendingEvents();

    List<BusEventOutbox> getPendingEvents(int limit);

    void markAsPublished(UUID outboxId);

    void markAsFailed(UUID outboxId, String reason);
}
