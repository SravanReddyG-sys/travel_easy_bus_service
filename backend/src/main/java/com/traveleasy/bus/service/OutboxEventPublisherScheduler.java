package com.traveleasy.bus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisherScheduler {

    private final EventOutboxService eventOutboxService;

    @Scheduled(fixedDelayString = "${bus.outbox.poll-interval-ms:5000}")
    public void publishPendingEventsJob() {
        try {
            eventOutboxService.processPendingEvents();
        } catch (Exception e) {
            log.error("Error during scheduled outbox event publishing: {}", e.getMessage(), e);
        }
    }
}
