package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.ConfirmSeatsRequest;
import com.traveleasy.bus.dto.request.LockSeatsRequest;
import com.traveleasy.bus.dto.request.ReleaseSeatsRequest;
import com.traveleasy.bus.dto.response.ConfirmSeatsResponse;
import com.traveleasy.bus.dto.response.LockSeatsResponse;
import com.traveleasy.bus.dto.response.ReleaseSeatsResponse;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.entity.ScheduleSeatInventory;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.event.SeatBookedEvent;
import com.traveleasy.bus.event.SeatLockedEvent;
import com.traveleasy.bus.event.SeatReleasedEvent;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.exception.SeatConflictException;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.service.EventOutboxService;
import com.traveleasy.bus.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatLockServiceImpl implements SeatLockService {

    private final ScheduleSeatInventoryRepository inventoryRepository;
    private final ScheduleRepository scheduleRepository;
    private final FareRepository fareRepository;
    private final EventOutboxService eventOutboxService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LockSeatsResponse lockSeats(UUID scheduleId, LockSeatsRequest request) {
        log.info("Locking seats {} on schedule {}", request.getSeatNumbers(), scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        if (schedule.getStatus() != ScheduleStatus.SCHEDULED || schedule.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.SCHEDULE_UNAVAILABLE,
                    "Schedule is not open for booking (status: " + schedule.getStatus() + ")");
        }

        List<String> upperSeats = request.getSeatNumbers().stream()
                .map(s -> s.trim().toUpperCase())
                .distinct()
                .collect(Collectors.toList());

        // Acquire DB Pessimistic Write Lock (SELECT ... FOR UPDATE)
        List<ScheduleSeatInventory> inventories = inventoryRepository
                .findByScheduleIdAndSeatNumbersForUpdate(scheduleId, upperSeats);

        if (inventories.size() < upperSeats.size()) {
            Set<String> found = inventories.stream().map(i -> i.getSeat().getSeatNumber()).collect(Collectors.toSet());
            List<String> missing = upperSeats.stream().filter(s -> !found.contains(s)).collect(Collectors.toList());
            throw new ResourceNotFoundException("Seats not found on schedule: " + missing);
        }

        LocalDateTime now = LocalDateTime.now();
        String lockToken = (request.getBookingSessionId() != null && !request.getBookingSessionId().trim().isEmpty())
                ? request.getBookingSessionId().trim()
                : "lock_" + UUID.randomUUID().toString();

        int ttl = (request.getTtlMinutes() != null && request.getTtlMinutes() > 0) ? request.getTtlMinutes() : 10;
        LocalDateTime expiresAt = now.plusMinutes(ttl);

        // Validate availability and conflict state
        for (ScheduleSeatInventory inv : inventories) {
            String seatNum = inv.getSeat().getSeatNumber();

            if (inv.getStatus() == SeatInventoryStatus.BOOKED) {
                throw SeatConflictException.alreadyBooked(seatNum);
            }
            if (inv.getStatus() == SeatInventoryStatus.BLOCKED) {
                throw SeatConflictException.blocked(seatNum);
            }
            if (inv.getStatus() == SeatInventoryStatus.LOCKED) {
                if (inv.getLockExpiryTime() != null && inv.getLockExpiryTime().isAfter(now)) {
                    // Valid lock held: check if owned by current session
                    if (!lockToken.equals(inv.getLockedByBookingSession())) {
                        throw SeatConflictException.alreadyLocked(seatNum, inv.getLockExpiryTime());
                    }
                }
            }
        }

        // Calculate Pricing
        List<Fare> fares = fareRepository.findByScheduleId(scheduleId);
        Map<SeatType, BigDecimal> priceMap = fares.stream()
                .collect(Collectors.toMap(Fare::getSeatType, Fare::getTotalAmount, (f1, f2) -> f1));

        BigDecimal totalAmount = BigDecimal.ZERO;
        String currency = "INR";

        for (ScheduleSeatInventory inv : inventories) {
            inv.setStatus(SeatInventoryStatus.LOCKED);
            inv.setLockedByBookingSession(lockToken);
            inv.setLockExpiryTime(expiresAt);

            SeatType st = inv.getSeat().getSeatType();
            BigDecimal seatPrice = priceMap.getOrDefault(st, BigDecimal.ZERO);
            totalAmount = totalAmount.add(seatPrice);
        }

        inventoryRepository.saveAll(inventories);

        // Publish Outbox Domain Event
        eventOutboxService.saveEvent(SeatLockedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(scheduleId)
                .eventType("SEAT_LOCKED")
                .scheduleId(scheduleId)
                .seatNumbers(upperSeats)
                .lockToken(lockToken)
                .lockExpiryTime(expiresAt)
                .correlationId(lockToken)
                .build());

        log.info("Successfully locked {} seats with token {} until {}", upperSeats.size(), lockToken, expiresAt);

        return LockSeatsResponse.builder()
                .scheduleId(scheduleId)
                .lockedSeats(upperSeats)
                .lockToken(lockToken)
                .lockExpiresAt(expiresAt)
                .totalAmount(totalAmount)
                .currency(currency)
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConfirmSeatsResponse confirmSeats(UUID scheduleId, ConfirmSeatsRequest request) {
        log.info("Confirming seats {} for booking reference {} on schedule {}",
                request.getSeatNumbers(), request.getBookingReference(), scheduleId);

        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found with ID: " + scheduleId);
        }

        List<String> upperSeats = request.getSeatNumbers().stream()
                .map(s -> s.trim().toUpperCase())
                .distinct()
                .collect(Collectors.toList());

        List<ScheduleSeatInventory> inventories = inventoryRepository
                .findByScheduleIdAndSeatNumbersForUpdate(scheduleId, upperSeats);

        if (inventories.size() < upperSeats.size()) {
            Set<String> found = inventories.stream().map(i -> i.getSeat().getSeatNumber()).collect(Collectors.toSet());
            List<String> missing = upperSeats.stream().filter(s -> !found.contains(s)).collect(Collectors.toList());
            throw new ResourceNotFoundException("Seats not found on schedule: " + missing);
        }

        LocalDateTime now = LocalDateTime.now();

        for (ScheduleSeatInventory inv : inventories) {
            String seatNum = inv.getSeat().getSeatNumber();

            if (inv.getStatus() == SeatInventoryStatus.BOOKED) {
                if (request.getBookingReference().equals(inv.getBookingReference())) {
                    continue; // Idempotent confirm
                }
                throw SeatConflictException.alreadyBooked(seatNum);
            }

            if (inv.getStatus() != SeatInventoryStatus.LOCKED) {
                throw new BusDomainException(HttpStatus.CONFLICT, ErrorCode.SEAT_NOT_AVAILABLE,
                        "Seat " + seatNum + " is not locked and cannot be confirmed (status: " + inv.getStatus() + ")");
            }

            if (!request.getLockToken().equals(inv.getLockedByBookingSession())) {
                throw new BusDomainException(HttpStatus.FORBIDDEN, ErrorCode.SEAT_LOCK_MISMATCH,
                        "Lock token does not match for seat " + seatNum);
            }

            if (inv.getLockExpiryTime() != null && inv.getLockExpiryTime().isBefore(now)) {
                throw new BusDomainException(HttpStatus.GONE, ErrorCode.SEAT_LOCK_EXPIRED,
                        "Lock has expired for seat " + seatNum);
            }

            inv.setStatus(SeatInventoryStatus.BOOKED);
            inv.setBookingReference(request.getBookingReference().trim());
            inv.setLockExpiryTime(null);
            inv.setLockedByBookingSession(null);
        }

        inventoryRepository.saveAll(inventories);

        // Publish Outbox Domain Event
        eventOutboxService.saveEvent(SeatBookedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(scheduleId)
                .eventType("SEAT_BOOKED")
                .scheduleId(scheduleId)
                .bookingReference(request.getBookingReference().trim())
                .seatNumbers(upperSeats)
                .correlationId(request.getBookingReference().trim())
                .build());

        log.info("Successfully confirmed {} booked seats for booking {}", upperSeats.size(), request.getBookingReference());

        return ConfirmSeatsResponse.builder()
                .scheduleId(scheduleId)
                .bookedSeats(upperSeats)
                .bookingReference(request.getBookingReference())
                .confirmedAt(now)
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReleaseSeatsResponse releaseSeats(UUID scheduleId, ReleaseSeatsRequest request) {
        log.info("Releasing seats {} on schedule {}", request.getSeatNumbers(), scheduleId);

        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found with ID: " + scheduleId);
        }

        List<String> upperSeats = request.getSeatNumbers().stream()
                .map(s -> s.trim().toUpperCase())
                .distinct()
                .collect(Collectors.toList());

        List<ScheduleSeatInventory> inventories = inventoryRepository
                .findByScheduleIdAndSeatNumbersForUpdate(scheduleId, upperSeats);

        LocalDateTime now = LocalDateTime.now();

        for (ScheduleSeatInventory inv : inventories) {
            String seatNum = inv.getSeat().getSeatNumber();

            if (inv.getStatus() == SeatInventoryStatus.LOCKED && request.getLockToken() != null) {
                if (!request.getLockToken().equals(inv.getLockedByBookingSession())) {
                    log.warn("Lock token mismatch releasing seat {}", seatNum);
                }
            }

            if (inv.getStatus() == SeatInventoryStatus.BOOKED && request.getBookingReference() != null) {
                if (!request.getBookingReference().equals(inv.getBookingReference())) {
                    log.warn("Booking reference mismatch releasing seat {}", seatNum);
                }
            }

            inv.setStatus(SeatInventoryStatus.AVAILABLE);
            inv.setLockExpiryTime(null);
            inv.setLockedByBookingSession(null);
            inv.setBookingReference(null);
        }

        inventoryRepository.saveAll(inventories);

        // Publish Outbox Domain Event
        eventOutboxService.saveEvent(SeatReleasedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(scheduleId)
                .eventType("SEAT_RELEASED")
                .scheduleId(scheduleId)
                .bookingReference(request.getBookingReference())
                .seatNumbers(upperSeats)
                .reason("Seats explicitly released")
                .correlationId(request.getLockToken())
                .build());

        log.info("Successfully released {} seats on schedule {}", upperSeats.size(), scheduleId);

        return ReleaseSeatsResponse.builder()
                .scheduleId(scheduleId)
                .releasedSeats(upperSeats)
                .releasedAt(now)
                .build();
    }

    @Override
    public int cleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduleSeatInventory> expiredLocks = inventoryRepository.findExpiredLocks(now);

        if (expiredLocks.isEmpty()) {
            return 0;
        }

        for (ScheduleSeatInventory inv : expiredLocks) {
            inv.setStatus(SeatInventoryStatus.AVAILABLE);
            inv.setLockExpiryTime(null);
            inv.setLockedByBookingSession(null);
        }

        inventoryRepository.saveAll(expiredLocks);

        Map<UUID, List<String>> bySchedule = expiredLocks.stream()
                .collect(Collectors.groupingBy(inv -> inv.getSchedule().getId(),
                        Collectors.mapping(inv -> inv.getSeat().getSeatNumber(), Collectors.toList())));

        bySchedule.forEach((schId, seats) -> {
            eventOutboxService.saveEvent(SeatReleasedEvent.builder()
                    .aggregateType("SCHEDULE")
                    .aggregateId(schId)
                    .eventType("SEAT_LOCK_EXPIRED")
                    .scheduleId(schId)
                    .seatNumbers(seats)
                    .reason("Seat lock TTL expired")
                    .build());
        });

        log.info("Cleaned up {} expired seat locks at {}", expiredLocks.size(), now);
        return expiredLocks.size();
    }
}
