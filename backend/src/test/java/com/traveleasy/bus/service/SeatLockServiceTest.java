package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.ConfirmSeatsRequest;
import com.traveleasy.bus.dto.request.LockSeatsRequest;
import com.traveleasy.bus.dto.request.ReleaseSeatsRequest;
import com.traveleasy.bus.dto.response.ConfirmSeatsResponse;
import com.traveleasy.bus.dto.response.LockSeatsResponse;
import com.traveleasy.bus.dto.response.ReleaseSeatsResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.entity.ScheduleSeatInventory;
import com.traveleasy.bus.entity.Seat;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.SeatConflictException;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.service.impl.SeatLockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private FareRepository fareRepository;

    @Mock
    private com.traveleasy.bus.service.EventOutboxService eventOutboxService;

    @InjectMocks
    private SeatLockServiceImpl seatLockService;

    private Schedule schedule;
    private Seat seat1A;
    private Seat seat1B;
    private ScheduleSeatInventory inv1A;
    private ScheduleSeatInventory inv1B;
    private Fare fare;

    @BeforeEach
    void setUp() {
        BusOperator operator = BusOperator.builder()
                .operatorCode("OP-LOCK-01")
                .companyName("Lock Express")
                .build();
        operator.setId(UUID.randomUUID());

        Bus bus = Bus.builder()
                .operator(operator)
                .busNumber("BUS-L1")
                .registrationNumber("DL-01-LC-9999")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .build();
        bus.setId(UUID.randomUUID());

        Route route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .build();
        route.setId(UUID.randomUUID());

        schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.of(2026, 12, 1))
                .departureTime(LocalDateTime.of(2026, 12, 1, 8, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 14, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();
        schedule.setId(UUID.randomUUID());

        seat1A = Seat.builder()
                .bus(bus)
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(1)
                .seatType(SeatType.SLEEPER)
                .isActive(true)
                .build();
        seat1A.setId(UUID.randomUUID());

        seat1B = Seat.builder()
                .bus(bus)
                .seatNumber("1B")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(2)
                .seatType(SeatType.SLEEPER)
                .isActive(true)
                .build();
        seat1B.setId(UUID.randomUUID());

        inv1A = ScheduleSeatInventory.builder()
                .schedule(schedule)
                .seat(seat1A)
                .status(SeatInventoryStatus.AVAILABLE)
                .build();
        inv1A.setId(UUID.randomUUID());

        inv1B = ScheduleSeatInventory.builder()
                .schedule(schedule)
                .seat(seat1B)
                .status(SeatInventoryStatus.AVAILABLE)
                .build();
        inv1B.setId(UUID.randomUUID());

        fare = Fare.builder()
                .schedule(schedule)
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("800.00"))
                .taxAmount(new BigDecimal("40.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("840.00"))
                .currency("INR")
                .build();
    }

    @Test
    void shouldLockSeatsSuccessfully() {
        LockSeatsRequest request = LockSeatsRequest.builder()
                .seatNumbers(List.of("1A", "1B"))
                .bookingSessionId("session_123")
                .ttlMinutes(10)
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), any()))
                .thenReturn(List.of(inv1A, inv1B));
        when(fareRepository.findByScheduleId(schedule.getId())).thenReturn(List.of(fare));

        LockSeatsResponse response = seatLockService.lockSeats(schedule.getId(), request);

        assertNotNull(response);
        assertEquals(2, response.getLockedSeats().size());
        assertEquals("session_123", response.getLockToken());
        assertEquals(new BigDecimal("1680.00"), response.getTotalAmount()); // 840 * 2
        assertEquals(SeatInventoryStatus.LOCKED, inv1A.getStatus());
        assertEquals(SeatInventoryStatus.LOCKED, inv1B.getStatus());
        assertEquals("session_123", inv1A.getLockedByBookingSession());
        assertNotNull(inv1A.getLockExpiryTime());

        verify(inventoryRepository).saveAll(any());
    }

    @Test
    void shouldRejectLockWhenSeatAlreadyBooked() {
        inv1A.setStatus(SeatInventoryStatus.BOOKED);
        inv1A.setBookingReference("BKG-EXISTING");

        LockSeatsRequest request = LockSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .bookingSessionId("session_123")
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), any()))
                .thenReturn(List.of(inv1A));

        assertThrows(SeatConflictException.class, () -> seatLockService.lockSeats(schedule.getId(), request));
        verify(inventoryRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectLockWhenSeatAlreadyLockedByAnotherSession() {
        inv1A.setStatus(SeatInventoryStatus.LOCKED);
        inv1A.setLockedByBookingSession("other_session_456");
        inv1A.setLockExpiryTime(LocalDateTime.now().plusMinutes(5)); // Active valid lock

        LockSeatsRequest request = LockSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .bookingSessionId("my_session_123")
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), any()))
                .thenReturn(List.of(inv1A));

        assertThrows(SeatConflictException.class, () -> seatLockService.lockSeats(schedule.getId(), request));
    }

    @Test
    void shouldConfirmLockedSeatsSuccessfully() {
        inv1A.setStatus(SeatInventoryStatus.LOCKED);
        inv1A.setLockedByBookingSession("token_xyz");
        inv1A.setLockExpiryTime(LocalDateTime.now().plusMinutes(5));

        ConfirmSeatsRequest request = ConfirmSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .lockToken("token_xyz")
                .bookingReference("BKG-CONF-001")
                .build();

        when(scheduleRepository.existsById(schedule.getId())).thenReturn(true);
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), any()))
                .thenReturn(List.of(inv1A));

        ConfirmSeatsResponse response = seatLockService.confirmSeats(schedule.getId(), request);

        assertNotNull(response);
        assertEquals(SeatInventoryStatus.BOOKED, inv1A.getStatus());
        assertEquals("BKG-CONF-001", inv1A.getBookingReference());
        assertNull(inv1A.getLockExpiryTime());
        assertNull(inv1A.getLockedByBookingSession());
        verify(inventoryRepository).saveAll(any());
    }

    @Test
    void shouldRejectConfirmWhenLockTokenMismatches() {
        inv1A.setStatus(SeatInventoryStatus.LOCKED);
        inv1A.setLockedByBookingSession("token_correct");
        inv1A.setLockExpiryTime(LocalDateTime.now().plusMinutes(5));

        ConfirmSeatsRequest request = ConfirmSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .lockToken("token_wrong")
                .bookingReference("BKG-CONF-001")
                .build();

        when(scheduleRepository.existsById(schedule.getId())).thenReturn(true);
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), any()))
                .thenReturn(List.of(inv1A));

        assertThrows(BusDomainException.class, () -> seatLockService.confirmSeats(schedule.getId(), request));
    }

    @Test
    void shouldReleaseLockedSeatsSuccessfully() {
        inv1A.setStatus(SeatInventoryStatus.LOCKED);
        inv1A.setLockedByBookingSession("token_xyz");
        inv1A.setLockExpiryTime(LocalDateTime.now().plusMinutes(5));

        ReleaseSeatsRequest request = ReleaseSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .lockToken("token_xyz")
                .build();

        when(scheduleRepository.existsById(schedule.getId())).thenReturn(true);
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), any()))
                .thenReturn(List.of(inv1A));

        ReleaseSeatsResponse response = seatLockService.releaseSeats(schedule.getId(), request);

        assertNotNull(response);
        assertEquals(SeatInventoryStatus.AVAILABLE, inv1A.getStatus());
        assertNull(inv1A.getLockExpiryTime());
        assertNull(inv1A.getLockedByBookingSession());
    }

    @Test
    void shouldCleanupExpiredLocks() {
        inv1A.setStatus(SeatInventoryStatus.LOCKED);
        inv1A.setLockExpiryTime(LocalDateTime.now().minusMinutes(2)); // Expired

        when(inventoryRepository.findExpiredLocks(any())).thenReturn(List.of(inv1A));

        int cleaned = seatLockService.cleanupExpiredLocks();

        assertEquals(1, cleaned);
        assertEquals(SeatInventoryStatus.AVAILABLE, inv1A.getStatus());
        assertNull(inv1A.getLockExpiryTime());
        verify(inventoryRepository).saveAll(any());
    }
}
