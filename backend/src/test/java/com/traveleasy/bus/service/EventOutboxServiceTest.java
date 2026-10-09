package com.traveleasy.bus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.entity.BusEventOutbox;
import com.traveleasy.bus.enums.EventStatus;
import com.traveleasy.bus.event.DomainEventPublisher;
import com.traveleasy.bus.event.SeatLockedEvent;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.repository.BusEventOutboxRepository;
import com.traveleasy.bus.service.impl.EventOutboxServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventOutboxServiceTest {

    @Mock
    private BusEventOutboxRepository outboxRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventOutboxServiceImpl eventOutboxService;

    private SeatLockedEvent sampleEvent;
    private BusEventOutbox sampleOutbox;

    @BeforeEach
    void setUp() {
        UUID scheduleId = UUID.randomUUID();
        sampleEvent = SeatLockedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("SEAT_LOCKED")
                .aggregateType("SCHEDULE")
                .aggregateId(scheduleId)
                .scheduleId(scheduleId)
                .seatNumbers(List.of("1A", "1B"))
                .lockToken("token-123")
                .lockExpiryTime(LocalDateTime.now().plusMinutes(10))
                .correlationId("corr-123")
                .build();

        sampleOutbox = BusEventOutbox.builder()
                .eventId(sampleEvent.getEventId())
                .eventType("SEAT_LOCKED")
                .aggregateType("SCHEDULE")
                .aggregateId(scheduleId)
                .correlationId("corr-123")
                .payload("{\"eventType\":\"SEAT_LOCKED\"}")
                .status(EventStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    @DisplayName("Should successfully serialize and save domain event into outbox table")
    void shouldSaveEventSuccessfully() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"SEAT_LOCKED\"}");
        when(outboxRepository.save(any(BusEventOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusEventOutbox saved = eventOutboxService.saveEvent(sampleEvent);

        assertThat(saved).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("SEAT_LOCKED");
        assertThat(saved.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        verify(outboxRepository, times(1)).save(any(BusEventOutbox.class));
    }

    @Test
    @DisplayName("Should throw BusDomainException if serialization fails")
    void shouldThrowExceptionWhenSerializationFails() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        assertThrows(BusDomainException.class, () -> eventOutboxService.saveEvent(sampleEvent));
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process pending outbox events and mark as PUBLISHED on successful dispatch")
    void shouldProcessPendingEventsSuccessfully() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(EventStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(sampleOutbox));
        when(outboxRepository.save(any(BusEventOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventOutboxService.processPendingEvents();

        verify(domainEventPublisher, times(1)).publish(any(), eq(sampleOutbox));
        assertThat(sampleOutbox.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(sampleOutbox.getPublishedAt()).isNotNull();
        verify(outboxRepository, times(1)).save(sampleOutbox);
    }

    @Test
    @DisplayName("Should increment retryCount when publisher fails and mark as FAILED after max retries")
    void shouldHandlePublisherFailureWithRetry() {
        sampleOutbox.setRetryCount(4);
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(EventStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(sampleOutbox));
        doThrow(new RuntimeException("Broker unreachable")).when(domainEventPublisher).publish(any(), any());
        when(outboxRepository.save(any(BusEventOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventOutboxService.processPendingEvents();

        assertThat(sampleOutbox.getRetryCount()).isEqualTo(5);
        assertThat(sampleOutbox.getStatus()).isEqualTo(EventStatus.FAILED);
        verify(outboxRepository, times(1)).save(sampleOutbox);
    }

    @Test
    @DisplayName("Should return pending events by limit")
    void shouldGetPendingEvents() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(EventStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(sampleOutbox));

        List<BusEventOutbox> pending = eventOutboxService.getPendingEvents(10);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo("SEAT_LOCKED");
    }

    @Test
    @DisplayName("Should mark outbox record as published by ID")
    void shouldMarkAsPublished() {
        UUID id = UUID.randomUUID();
        when(outboxRepository.findById(id)).thenReturn(Optional.of(sampleOutbox));

        eventOutboxService.markAsPublished(id);

        assertThat(sampleOutbox.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(outboxRepository, times(1)).save(sampleOutbox);
    }

    @Test
    @DisplayName("Should mark outbox record as failed by ID")
    void shouldMarkAsFailed() {
        UUID id = UUID.randomUUID();
        when(outboxRepository.findById(id)).thenReturn(Optional.of(sampleOutbox));

        eventOutboxService.markAsFailed(id, "Permanent schema error");

        assertThat(sampleOutbox.getStatus()).isEqualTo(EventStatus.FAILED);
        verify(outboxRepository, times(1)).save(sampleOutbox);
    }
}
