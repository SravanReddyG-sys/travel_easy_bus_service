package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class BusDomainRepositoryTest {

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
    private RouteStopRepository routeStopRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private FareRepository fareRepository;

    private BusOperator operator;

    @BeforeEach
    void setUp() {
        operator = BusOperator.builder()
                .operatorCode("EXP-BUS-01")
                .companyName("Express Bus Lines")
                .contactEmail("contact@expressbus.com")
                .contactPhone("+919876543210")
                .isActive(true)
                .build();
        operator = operatorRepository.save(operator);
    }

    @Test
    void shouldPersistBusWithPhysicalSeatsAndAmenities() {
        Bus bus = Bus.builder()
                .operator(operator)
                .busNumber("EX-101")
                .registrationNumber("DL-01-AB-1234")
                .busType(BusType.VOLVO_MULTI_AXLE)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .amenities(List.of("WiFi", "Charging Port", "Water Bottle"))
                .build();
        bus = busRepository.save(bus);

        Seat seat1 = Seat.builder()
                .bus(bus)
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(1)
                .seatType(SeatType.WINDOW)
                .isActive(true)
                .build();

        Seat seat2 = Seat.builder()
                .bus(bus)
                .seatNumber("1B")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(2)
                .seatType(SeatType.AISLE)
                .isActive(true)
                .build();

        seatRepository.saveAll(List.of(seat1, seat2));
        entityManager.flush();
        entityManager.clear();

        Optional<Bus> foundBus = busRepository.findById(bus.getId());
        assertTrue(foundBus.isPresent());
        assertEquals("DL-01-AB-1234", foundBus.get().getRegistrationNumber());
        assertEquals(3, foundBus.get().getAmenities().size());

        List<Seat> seats = seatRepository.findByBusIdOrderByRowNumAscColNumAsc(bus.getId());
        assertEquals(2, seats.size());
        assertEquals("1A", seats.get(0).getSeatNumber());
        assertEquals("1B", seats.get(1).getSeatNumber());
    }

    @Test
    void shouldPersistRouteWithOrderedStopsAndPoints() {
        Route route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI-01")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(330)
                .isActive(true)
                .build();
        route = routeRepository.save(route);

        RouteStop stop1 = RouteStop.builder()
                .route(route)
                .stopName("Gurgaon IFFCO Chowk")
                .stopOrder(1)
                .distanceFromOriginKm(30.0)
                .build();

        RouteStop stop2 = RouteStop.builder()
                .route(route)
                .stopName("Kotputli Highway Junction")
                .stopOrder(2)
                .distanceFromOriginKm(140.0)
                .build();

        routeStopRepository.saveAll(List.of(stop1, stop2));
        entityManager.flush();
        entityManager.clear();

        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
        assertEquals(2, stops.size());
        assertEquals(1, stops.get(0).getStopOrder());
        assertEquals("Gurgaon IFFCO Chowk", stops.get(0).getStopName());
        assertEquals(2, stops.get(1).getStopOrder());
    }

    @Test
    void shouldPersistScheduleAndFares() {
        Bus bus = busRepository.save(Bus.builder()
                .operator(operator)
                .busNumber("EX-102")
                .registrationNumber("DL-01-CD-5678")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(30)
                .build());

        Route route = routeRepository.save(Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI-02")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(300)
                .build());

        LocalDate today = LocalDate.now();
        LocalDateTime departure = today.atTime(22, 0);
        LocalDateTime arrival = today.plusDays(1).atTime(3, 30);

        Schedule schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(today)
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();
        schedule = scheduleRepository.save(schedule);

        Fare fare = Fare.builder()
                .schedule(schedule)
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("1200.00"))
                .taxAmount(new BigDecimal("60.00"))
                .discountAmount(new BigDecimal("0.00"))
                .totalAmount(new BigDecimal("1260.00"))
                .currency("INR")
                .build();
        fareRepository.save(fare);

        entityManager.flush();
        entityManager.clear();

        List<Schedule> available = scheduleRepository.findAvailableSchedulesForSearch("Delhi", "Jaipur", today);
        assertEquals(1, available.size());
        assertEquals(schedule.getId(), available.get(0).getId());

        List<Fare> fares = fareRepository.findByScheduleId(schedule.getId());
        assertEquals(1, fares.size());
        assertEquals(new BigDecimal("1260.00"), fares.get(0).getTotalAmount());
    }
}
