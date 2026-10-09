package com.traveleasy.bus.integration.booking;

import com.traveleasy.bus.entity.*;
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
import com.traveleasy.bus.integration.booking.dto.*;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.service.EventOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusBookingIntegrationServiceImpl implements BusBookingIntegrationService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatInventoryRepository inventoryRepository;
    private final FareRepository fareRepository;
    private final EventOutboxService eventOutboxService;

    @Override
    @Transactional
    public BookingSeatHoldResponse holdSeats(BookingSeatHoldRequest request) {
        log.info("Integration: Holding seats {} on schedule {}", request.getSeatNumbers(), request.getScheduleId());

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + request.getScheduleId()));

        if (schedule.getStatus() == ScheduleStatus.CANCELLED || schedule.getAvailabilityStatus() == AvailabilityStatus.UNAVAILABLE) {
            throw new BusDomainException(HttpStatus.CONFLICT, ErrorCode.SCHEDULE_UNAVAILABLE,
                    "Schedule is not open or available for bookings");
        }

        // Acquire DB row-level pessimistic write lock
        List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(
                request.getScheduleId(), request.getSeatNumbers());

        if (inventories.size() != request.getSeatNumbers().size()) {
            throw new ResourceNotFoundException("One or more specified seats were not found on this schedule");
        }

        LocalDateTime now = LocalDateTime.now();
        for (ScheduleSeatInventory inv : inventories) {
            if (inv.getStatus() == SeatInventoryStatus.BOOKED) {
                throw new SeatConflictException("Seat " + inv.getSeat().getSeatNumber() + " is already booked");
            }
            if (inv.getStatus() == SeatInventoryStatus.BLOCKED) {
                throw new SeatConflictException("Seat " + inv.getSeat().getSeatNumber() + " is blocked by operator");
            }
            if (inv.getStatus() == SeatInventoryStatus.LOCKED && inv.getLockExpiryTime() != null && inv.getLockExpiryTime().isAfter(now)) {
                throw new SeatConflictException("Seat " + inv.getSeat().getSeatNumber() + " is currently held by another checkout session");
            }
        }

        String lockToken = "HOLD-" + UUID.randomUUID().toString();
        int holdDuration = request.getHoldDurationMinutes() > 0 ? request.getHoldDurationMinutes() : 10;
        LocalDateTime expiryTime = now.plusMinutes(holdDuration);

        for (ScheduleSeatInventory inv : inventories) {
            inv.setStatus(SeatInventoryStatus.LOCKED);
            inv.setLockedByBookingSession(lockToken);
            inv.setLockExpiryTime(expiryTime);
        }

        inventoryRepository.saveAll(inventories);

        // Calculate Pricing Breakdown
        Map<SeatType, Fare> fareMap = loadFareMap(schedule);
        List<SeatPriceBreakdown> priceBreakdowns = new ArrayList<>();
        BigDecimal totalHoldAmount = BigDecimal.ZERO;

        for (ScheduleSeatInventory inv : inventories) {
            SeatType seatType = inv.getSeat().getSeatType();
            Fare fare = fareMap.get(seatType);

            BigDecimal base = fare != null ? fare.getBaseAmount() : BigDecimal.ZERO;
            BigDecimal tax = fare != null ? fare.getTaxAmount() : BigDecimal.ZERO;
            BigDecimal total = fare != null ? fare.getTotalAmount() : BigDecimal.ZERO;

            priceBreakdowns.add(SeatPriceBreakdown.builder()
                    .seatNumber(inv.getSeat().getSeatNumber())
                    .seatType(seatType)
                    .baseAmount(base)
                    .taxAmount(tax)
                    .totalAmount(total)
                    .build());

            totalHoldAmount = totalHoldAmount.add(total);
        }

        // Outbox event
        eventOutboxService.saveEvent(SeatLockedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(schedule.getId())
                .eventType("SEAT_LOCKED")
                .scheduleId(schedule.getId())
                .seatNumbers(request.getSeatNumbers())
                .lockToken(lockToken)
                .lockExpiryTime(expiryTime)
                .correlationId(lockToken)
                .build());

        return BookingSeatHoldResponse.builder()
                .lockToken(lockToken)
                .scheduleId(schedule.getId())
                .lockedSeats(request.getSeatNumbers())
                .lockExpiryTime(expiryTime)
                .seatPrices(priceBreakdowns)
                .totalHoldAmount(totalHoldAmount)
                .currency("INR")
                .build();
    }

    @Override
    @Transactional
    public BookingSeatConfirmResponse confirmSeats(BookingSeatConfirmRequest request) {
        log.info("Integration: Confirming seats {} for booking {} with lockToken {}",
                request.getSeatNumbers(), request.getBookingReference(), request.getLockToken());

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + request.getScheduleId()));

        List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(
                request.getScheduleId(), request.getSeatNumbers());

        if (inventories.size() != request.getSeatNumbers().size()) {
            throw new ResourceNotFoundException("One or more specified seats were not found on this schedule");
        }

        LocalDateTime now = LocalDateTime.now();
        for (ScheduleSeatInventory inv : inventories) {
            if (inv.getStatus() == SeatInventoryStatus.BOOKED) {
                if (request.getBookingReference().equals(inv.getBookingReference())) {
                    continue; // Idempotent replay for same booking
                }
                throw new SeatConflictException("Seat " + inv.getSeat().getSeatNumber() + " is already booked by another reservation");
            }

            if (inv.getStatus() != SeatInventoryStatus.LOCKED) {
                throw new SeatConflictException("Seat " + inv.getSeat().getSeatNumber() + " is not in LOCKED status");
            }

            if (!request.getLockToken().equals(inv.getLockedByBookingSession())) {
                throw new SeatConflictException("Lock token mismatch for seat " + inv.getSeat().getSeatNumber());
            }

            if (inv.getLockExpiryTime() != null && inv.getLockExpiryTime().isBefore(now)) {
                throw new SeatConflictException("Seat lock has expired for seat " + inv.getSeat().getSeatNumber());
            }
        }

        Map<SeatType, Fare> fareMap = loadFareMap(schedule);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ScheduleSeatInventory inv : inventories) {
            inv.setStatus(SeatInventoryStatus.BOOKED);
            inv.setBookingReference(request.getBookingReference());
            inv.setLockedByBookingSession(null);
            inv.setLockExpiryTime(null);

            Fare fare = fareMap.get(inv.getSeat().getSeatType());
            if (fare != null && fare.getTotalAmount() != null) {
                totalAmount = totalAmount.add(fare.getTotalAmount());
            }
        }

        inventoryRepository.saveAll(inventories);

        // Outbox event
        eventOutboxService.saveEvent(SeatBookedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(schedule.getId())
                .eventType("SEAT_BOOKED")
                .scheduleId(schedule.getId())
                .bookingReference(request.getBookingReference())
                .seatNumbers(request.getSeatNumbers())
                .totalAmount(totalAmount)
                .currency("INR")
                .correlationId(request.getBookingReference())
                .build());

        return BookingSeatConfirmResponse.builder()
                .confirmed(true)
                .bookingReference(request.getBookingReference())
                .scheduleId(schedule.getId())
                .confirmedSeats(request.getSeatNumbers())
                .totalAmount(totalAmount)
                .currency("INR")
                .departureTime(schedule.getDepartureTime())
                .busRegistrationNumber(schedule.getBus().getRegistrationNumber())
                .origin(schedule.getRoute().getOrigin())
                .destination(schedule.getRoute().getDestination())
                .confirmedAt(now)
                .build();
    }

    @Override
    @Transactional
    public BookingSeatCancelResponse cancelBooking(BookingSeatCancelRequest request) {
        log.info("Integration: Cancelling booking {} for schedule {}",
                request.getBookingReference(), request.getScheduleId());

        List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdAndBookingReference(
                request.getScheduleId(), request.getBookingReference());

        if (inventories.isEmpty()) {
            inventories = inventoryRepository.findByBookingReference(request.getBookingReference());
        }

        if (inventories.isEmpty()) {
            throw new ResourceNotFoundException("No booked seats found for booking reference: " + request.getBookingReference());
        }

        List<ScheduleSeatInventory> targetInventories = inventories;
        if (request.getSeatNumbers() != null && !request.getSeatNumbers().isEmpty()) {
            targetInventories = inventories.stream()
                    .filter(inv -> request.getSeatNumbers().contains(inv.getSeat().getSeatNumber()))
                    .collect(Collectors.toList());
        }

        List<String> releasedSeatNumbers = new ArrayList<>();
        for (ScheduleSeatInventory inv : targetInventories) {
            inv.setStatus(SeatInventoryStatus.AVAILABLE);
            inv.setBookingReference(null);
            inv.setLockedByBookingSession(null);
            inv.setLockExpiryTime(null);
            releasedSeatNumbers.add(inv.getSeat().getSeatNumber());
        }

        inventoryRepository.saveAll(targetInventories);

        // Outbox event
        eventOutboxService.saveEvent(SeatReleasedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(request.getScheduleId())
                .eventType("SEAT_RELEASED")
                .scheduleId(request.getScheduleId())
                .bookingReference(request.getBookingReference())
                .seatNumbers(releasedSeatNumbers)
                .reason(request.getCancellationReason() != null ? request.getCancellationReason() : "Booking cancelled by user")
                .correlationId(request.getBookingReference())
                .build());

        return BookingSeatCancelResponse.builder()
                .cancelled(true)
                .bookingReference(request.getBookingReference())
                .scheduleId(request.getScheduleId())
                .releasedSeats(releasedSeatNumbers)
                .refundEligible(true)
                .message("Booking cancelled successfully and seats released")
                .cancelledAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatusResponse getBookingStatus(String bookingReference) {
        List<ScheduleSeatInventory> inventories = inventoryRepository.findByBookingReference(bookingReference);
        if (inventories.isEmpty()) {
            throw new ResourceNotFoundException("No records found for booking reference: " + bookingReference);
        }

        Schedule schedule = inventories.get(0).getSchedule();
        List<String> seats = inventories.stream()
                .map(inv -> inv.getSeat().getSeatNumber())
                .collect(Collectors.toList());

        String status = inventories.get(0).getStatus().name();

        return BookingStatusResponse.builder()
                .bookingReference(bookingReference)
                .scheduleId(schedule.getId())
                .busNumber(schedule.getBus().getBusNumber())
                .registrationNumber(schedule.getBus().getRegistrationNumber())
                .origin(schedule.getRoute().getOrigin())
                .destination(schedule.getRoute().getDestination())
                .departureTime(schedule.getDepartureTime())
                .seatNumbers(seats)
                .status(status)
                .build();
    }

    private Map<SeatType, Fare> loadFareMap(Schedule schedule) {
        Map<SeatType, Fare> fareMap = new HashMap<>();
        List<Fare> fares = schedule.getFares() != null && !schedule.getFares().isEmpty() ?
                schedule.getFares() : fareRepository.findByScheduleId(schedule.getId());

        for (Fare f : fares) {
            if (f.getSeatType() != null) {
                fareMap.put(f.getSeatType(), f);
            }
        }
        return fareMap;
    }
}
