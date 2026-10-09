package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ScheduleSeatInventoryPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BusOperatorRepository operatorRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSeatInventoryRepository inventoryRepository;

    private Bus bus;
    private Seat physicalSeat12A;
    private Route route;
    private BusOperator operator;

    @BeforeEach
    void setUp() {
        operator = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP-TEST-01")
                .companyName("Test Lines")
                .contactEmail("test@testlines.com")
                .contactPhone("+919000000000")
                .build());

        bus = busRepository.save(Bus.builder()
                .operator(operator)
                .busNumber("BUS-101")
                .registrationNumber("KA-01-EQ-9999")
                .busType(BusType.AC_SEATER)
                .totalSeats(1)
                .build());

        physicalSeat12A = seatRepository.save(Seat.builder()
                .bus(bus)
                .seatNumber("12A")
                .deck(Deck.LOWER)
                .rowNum(3)
                .colNum(1)
                .seatType(SeatType.WINDOW)
                .isActive(true)
                .build());

        route = routeRepository.save(Route.builder()
                .operator(operator)
                .routeCode("RT-BLR-CHE")
                .origin("Bangalore")
                .destination("Chennai")
                .distanceKm(350.0)
                .estimatedDurationMinutes(360)
                .build());
    }

    @Test
    void shouldProvePhysicalSeatCanBeBookedOnSchedule1AndAvailableOnSchedule2() {
        LocalDate date1 = LocalDate.of(2026, 10, 20);
        LocalDate date2 = LocalDate.of(2026, 10, 21);

        Schedule schedule1 = scheduleRepository.save(Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(date1)
                .departureTime(date1.atTime(6, 0))
                .arrivalTime(date1.atTime(12, 0))
                .build());

        Schedule schedule2 = scheduleRepository.save(Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(date2)
                .departureTime(date2.atTime(6, 0))
                .arrivalTime(date2.atTime(12, 0))
                .build());

        // Inventory for Schedule 1: Seat 12A is BOOKED
        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder()
                .schedule(schedule1)
                .seat(physicalSeat12A)
                .status(SeatInventoryStatus.BOOKED)
                .bookingReference("BKG-OCT20-001")
                .build();

        // Inventory for Schedule 2: The EXACT SAME physical Seat 12A is AVAILABLE
        ScheduleSeatInventory inv2 = ScheduleSeatInventory.builder()
                .schedule(schedule2)
                .seat(physicalSeat12A)
                .status(SeatInventoryStatus.AVAILABLE)
                .build();

        inventoryRepository.saveAll(List.of(inv1, inv2));
        entityManager.flush();
        entityManager.clear();

        // 1. Verify Schedule 1 inventory status
        Optional<ScheduleSeatInventory> s1Seat = inventoryRepository.findByScheduleIdAndSeatNumber(schedule1.getId(), "12A");
        assertTrue(s1Seat.isPresent());
        assertEquals(SeatInventoryStatus.BOOKED, s1Seat.get().getStatus());
        assertEquals("BKG-OCT20-001", s1Seat.get().getBookingReference());
        assertEquals(physicalSeat12A.getId(), s1Seat.get().getSeat().getId());

        // 2. Verify Schedule 2 inventory status
        Optional<ScheduleSeatInventory> s2Seat = inventoryRepository.findByScheduleIdAndSeatNumber(schedule2.getId(), "12A");
        assertTrue(s2Seat.isPresent());
        assertEquals(SeatInventoryStatus.AVAILABLE, s2Seat.get().getStatus());
        assertNull(s2Seat.get().getBookingReference());
        assertEquals(physicalSeat12A.getId(), s2Seat.get().getSeat().getId());

        // 3. Verify physical seat remains clean without schedule state
        Seat seatInDb = seatRepository.findById(physicalSeat12A.getId()).orElseThrow();
        assertEquals("12A", seatInDb.getSeatNumber());
        assertTrue(seatInDb.getIsActive());
    }

    @Test
    void shouldEnforceUniqueConstraintOnBusAndSeatNumber() {
        // Attempting to create duplicate seat "12A" on the same bus should fail
        Seat duplicateSeat = Seat.builder()
                .bus(bus)
                .seatNumber("12A")
                .deck(Deck.LOWER)
                .rowNum(3)
                .colNum(1)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            seatRepository.saveAndFlush(duplicateSeat);
        });
    }

    @Test
    void shouldEnforceUniqueConstraintOnScheduleAndSeatInventory() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(date)
                .departureTime(date.atTime(6, 0))
                .arrivalTime(date.atTime(12, 0))
                .build());

        ScheduleSeatInventory inv1 = inventoryRepository.save(ScheduleSeatInventory.builder()
                .schedule(schedule)
                .seat(physicalSeat12A)
                .status(SeatInventoryStatus.AVAILABLE)
                .build());

        entityManager.flush();

        // Attempting to insert another inventory record for the same (schedule, seat) must fail
        ScheduleSeatInventory duplicateInv = ScheduleSeatInventory.builder()
                .schedule(schedule)
                .seat(physicalSeat12A)
                .status(SeatInventoryStatus.LOCKED)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            inventoryRepository.saveAndFlush(duplicateInv);
        });
    }
}
