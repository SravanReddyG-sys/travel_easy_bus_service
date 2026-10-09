package com.traveleasy.bus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockCleanupScheduler {

    private final SeatLockService seatLockService;

    /**
     * Periodically scans and resets expired seat locks every 60 seconds.
     */
    @Scheduled(fixedRateString = "${app.seat-lock.cleanup-interval-ms:60000}")
    public void cleanupExpiredLocks() {
        try {
            int cleanedCount = seatLockService.cleanupExpiredLocks();
            if (cleanedCount > 0) {
                log.info("SeatLockCleanupScheduler: Cleaned up {} expired seat locks", cleanedCount);
            }
        } catch (Exception e) {
            log.error("Error running scheduled seat lock cleanup: {}", e.getMessage(), e);
        }
    }
}
