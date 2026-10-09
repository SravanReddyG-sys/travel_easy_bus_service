package com.traveleasy.bus.event;

import com.traveleasy.bus.entity.BusEventOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(BusDomainEvent event, BusEventOutbox outboxRecord) {
        log.info("[EVENT_PUBLISHER] Successfully dispatched domain event [type={}, aggregateType={}, aggregateId={}, eventId={}] to message broker topic 'traveleasy.bus.events'",
                outboxRecord.getEventType(), outboxRecord.getAggregateType(), outboxRecord.getAggregateId(), outboxRecord.getEventId());
    }
}
