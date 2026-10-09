package com.traveleasy.bus.integration.booking;

import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
import com.traveleasy.bus.exception.SeatConflictException;
import com.traveleasy.bus.integration.booking.dto.*;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusBookingIntegrationServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Mock
    private FareRepository fareRepository;

    @Mock
    private com.traveleasy.bus.service.EventOutboxService eventOutboxService;

    @InjectMocks
    private BusBookingIntegrationServiceImpl integrationService;

    private Schedule schedule;
    private Seat seat1;
    private Seat seat2;
    private ScheduleSeatInventory inv1;
    private ScheduleSeatInventory inv2;

    @BeforeEach
    void setUp() {
        BusOperator operator = BusOperator.builder().operatorCode("OP-INT").companyName("Int Express").build();
        operator.setId(UUID.randomUUID());

        Bus bus = Bus.builder().operator(operator).busNumber("BUS-INT-01").registrationNumber("DL-01-INT-1111").busType(BusType.AC_SEATER).totalSeats(2).build();
        bus.setId(UUID.randomUUID());

        Route route = Route.builder().operator(operator).routeCode("RT-INT").origin("Delhi").destination("Jaipur").build();
        route.setId(UUID.randomUUID());

        schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.now())
                .departureTime(LocalDateTime.now().plusHours(4))
                .arrivalTime(LocalDateTime.now().plusHours(9))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .fares(new ArrayList<>())
                .build();
        schedule.setId(UUID.randomUUID());

        Fare fare = Fare.builder()
                .schedule(schedule)
                .seatType(SeatType.SEATER)
                .baseAmount(BigDecimal.valueOf(500))
                .taxAmount(BigDecimal.valueOf(25))
                .totalAmount(BigDecimal.valueOf(525))
                .currency("INR")
                .build();
        schedule.getFares().add(fare);

        seat1 = Seat.builder().bus(bus).seatNumber("1A").seatType(SeatType.SEATER).build();
        seat2 = Seat.builder().bus(bus).seatNumber("1B").seatType(SeatType.SEATER).build();

        inv1 = ScheduleSeatInventory.builder().schedule(schedule).seat(seat1).status(SeatInventoryStatus.AVAILABLE).build();
        inv2 = ScheduleSeatInventory.builder().schedule(schedule).seat(seat2).status(SeatInventoryStatus.AVAILABLE).build();
    }

    @Test
    @DisplayName("Should successfully hold seats and return lock token and price breakdown")
    void holdSeats_Success() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), eq(List.of("1A", "1B"))))
                .thenReturn(List.of(inv1, inv2));

        BookingSeatHoldRequest request = BookingSeatHoldRequest.builder()
                .scheduleId(schedule.getId())
                .seatNumbers(List.of("1A", "1B"))
                .holdDurationMinutes(10)
                .build();

        BookingSeatHoldResponse response = integrationService.holdSeats(request);

        assertThat(response).isNotNull();
        assertThat(response.getLockToken()).startsWith("HOLD-");
        assertThat(response.getLockedSeats()).containsExactly("1A", "1B");
        assertThat(response.getTotalHoldAmount()).isEqualByComparingTo("1050.00");
        assertThat(inv1.getStatus()).isEqualTo(SeatInventoryStatus.LOCKED);
        assertThat(inv2.getStatus()).isEqualTo(SeatInventoryStatus.LOCKED);
    }

    @Test
    @DisplayName("Should reject hold request when a seat is already booked")
    void holdSeats_ConflictWhenAlreadyBooked() {
        inv1.setStatus(SeatInventoryStatus.BOOKED);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), eq(List.of("1A"))))
                .thenReturn(List.of(inv1));

        BookingSeatHoldRequest request = BookingSeatHoldRequest.builder()
                .scheduleId(schedule.getId())
                .seatNumbers(List.of("1A"))
                .build();

        assertThrows(SeatConflictException.class, () -> integrationService.holdSeats(request));
    }

    @Test
    @DisplayName("Should confirm booking seats and transition to BOOKED")
    void confirmSeats_Success() {
        inv1.setStatus(SeatInventoryStatus.LOCKED);
        inv1.setLockedByBookingSession("HOLD-TOKEN-123");
        inv1.setLockExpiryTime(LocalDateTime.now().plusMinutes(5));

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumbersForUpdate(eq(schedule.getId()), eq(List.of("1A"))))
                .thenReturn(List.of(inv1));

        BookingSeatConfirmRequest request = BookingSeatConfirmRequest.builder()
                .scheduleId(schedule.getId())
                .bookingReference("BKG-999")
                .lockToken("HOLD-TOKEN-123")
                .seatNumbers(List.of("1A"))
                .build();

        BookingSeatConfirmResponse response = integrationService.confirmSeats(request);

        assertThat(response).isNotNull();
        assertThat(response.isConfirmed()).isTrue();
        assertThat(response.getBookingReference()).isEqualTo("BKG-999");
        assertThat(inv1.getStatus()).isEqualTo(SeatInventoryStatus.BOOKED);
        assertThat(inv1.getBookingReference()).isEqualTo("BKG-999");
    }

    @Test
    @DisplayName("Should cancel booking and restore seats to AVAILABLE")
    void cancelBooking_Success() {
        inv1.setStatus(SeatInventoryStatus.BOOKED);
        inv1.setBookingReference("BKG-999");

        when(inventoryRepository.findByScheduleIdAndBookingReference(schedule.getId(), "BKG-999"))
                .thenReturn(List.of(inv1));

        BookingSeatCancelRequest request = BookingSeatCancelRequest.builder()
                .scheduleId(schedule.getId())
                .bookingReference("BKG-999")
                .build();

        BookingSeatCancelResponse response = integrationService.cancelBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.isCancelled()).isTrue();
        assertThat(response.getReleasedSeats()).containsExactly("1A");
        assertThat(inv1.getStatus()).isEqualTo(SeatInventoryStatus.AVAILABLE);
        assertThat(inv1.getBookingReference()).isNull();
    }
}
