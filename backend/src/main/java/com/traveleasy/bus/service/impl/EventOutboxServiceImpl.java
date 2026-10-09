package com.traveleasy.bus.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.entity.BusEventOutbox;
import com.traveleasy.bus.enums.EventStatus;
import com.traveleasy.bus.event.BusDomainEvent;
import com.traveleasy.bus.event.DomainEventPublisher;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.repository.BusEventOutboxRepository;
import com.traveleasy.bus.service.EventOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventOutboxServiceImpl implements EventOutboxService {

    private final BusEventOutboxRepository outboxRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 5;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public BusEventOutbox saveEvent(BusDomainEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            BusEventOutbox outbox = BusEventOutbox.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .correlationId(event.getCorrelationId())
                    .payload(jsonPayload)
                    .status(EventStatus.PENDING)
                    .retryCount(0)
                    .build();

            log.info("Persisting outbox event [type={}, id={}, aggregateId={}]",
                    outbox.getEventType(), outbox.getEventId(), outbox.getAggregateId());
            return outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload to JSON: {}", e.getMessage(), e);
            throw new BusDomainException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to serialize event payload: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processPendingEvents() {
        List<BusEventOutbox> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
                EventStatus.PENDING, PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pendingEvents.size());

        for (BusEventOutbox outbox : pendingEvents) {
            try {
                domainEventPublisher.publish(null, outbox);
                outbox.setStatus(EventStatus.PUBLISHED);
                outbox.setPublishedAt(Instant.now());
                outboxRepository.save(outbox);
            } catch (Exception e) {
                log.error("Failed to publish outbox event [id={}]: {}", outbox.getEventId(), e.getMessage(), e);
                int retries = (outbox.getRetryCount() != null ? outbox.getRetryCount() : 0) + 1;
                outbox.setRetryCount(retries);
                if (retries >= MAX_RETRIES) {
                    outbox.setStatus(EventStatus.FAILED);
                }
                outboxRepository.save(outbox);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusEventOutbox> getPendingEvents(int limit) {
        return outboxRepository.findByStatusOrderByCreatedAtAsc(EventStatus.PENDING, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markAsPublished(UUID outboxId) {
        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.setStatus(EventStatus.PUBLISHED);
            outbox.setPublishedAt(Instant.now());
            outboxRepository.save(outbox);
        });
    }

    @Override
    @Transactional
    public void markAsFailed(UUID outboxId, String reason) {
        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.setStatus(EventStatus.FAILED);
            outboxRepository.save(outbox);
        });
    }
}
