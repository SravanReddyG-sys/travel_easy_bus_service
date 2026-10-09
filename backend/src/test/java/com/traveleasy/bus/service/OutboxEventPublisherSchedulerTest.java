package com.traveleasy.bus.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherSchedulerTest {

    @Mock
    private EventOutboxService eventOutboxService;

    @InjectMocks
    private OutboxEventPublisherScheduler scheduler;

    @Test
    @DisplayName("Should invoke processPendingEvents on scheduler execution")
    void shouldInvokeProcessPendingEvents() {
        scheduler.publishPendingEventsJob();
        verify(eventOutboxService, times(1)).processPendingEvents();
    }

    @Test
    @DisplayName("Should handle exception gracefully without throwing")
    void shouldHandleExceptionGracefully() {
        doThrow(new RuntimeException("Database error")).when(eventOutboxService).processPendingEvents();
        scheduler.publishPendingEventsJob();
        verify(eventOutboxService, times(1)).processPendingEvents();
    }
}
