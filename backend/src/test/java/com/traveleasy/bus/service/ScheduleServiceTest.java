package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.CreateScheduleRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleRequest;
import com.traveleasy.bus.dto.response.FareResponse;
import com.traveleasy.bus.dto.response.ScheduleResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatMapResponse;
import com.traveleasy.bus.dto.response.TravellerScheduleDetailResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.entity.Seat;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.InvalidScheduleException;
import com.traveleasy.bus.mapper.RouteMapper;
import com.traveleasy.bus.mapper.ScheduleMapper;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.RouteRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.repository.SeatRepository;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleSeatInventoryRepository scheduleSeatInventoryRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatService seatService;

    @Mock
    private FareService fareService;

    @Mock
    private com.traveleasy.bus.service.EventOutboxService eventOutboxService;

    @Spy
    private RouteMapper routeMapper = new RouteMapper();

    @Spy
    private ScheduleMapper scheduleMapper = new ScheduleMapper();

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private UUID operatorId;
    private BusOperator operator;
    private Bus bus;
    private Route route;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        operator = BusOperator.builder()
                .operatorCode("OP-EXP-01")
                .companyName("Express Lines")
                .build();
        operator.setId(operatorId);

        bus = Bus.builder()
                .operator(operator)
                .busNumber("BUS-001")
                .registrationNumber("DL-01-AB-1234")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .build();
        bus.setId(UUID.randomUUID());

        route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(330)
                .isActive(true)
                .build();
        route.setId(UUID.randomUUID());

        schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.of(2026, 10, 15))
                .departureTime(LocalDateTime.of(2026, 10, 15, 8, 0))
                .arrivalTime(LocalDateTime.of(2026, 10, 15, 14, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();
        schedule.setId(UUID.randomUUID());

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("operator@express.com")
                .operatorId(operatorId)
                .roles(Set.of(UserRole.ROLE_OPERATOR))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateScheduleSuccessfully_AndInitializeSeatInventory() {
        LocalDate travelDate = LocalDate.of(2026, 10, 15);
        LocalDateTime departure = LocalDateTime.of(2026, 10, 15, 8, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 10, 15, 14, 0);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus.getId())
                .routeId(route.getId())
                .scheduleDate(travelDate)
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        Seat seat1 = Seat.builder()
                .bus(bus)
                .seatNumber("U1")
                .deck(Deck.LOWER)
                .seatType(SeatType.SLEEPER)
                .rowNum(1)
                .colNum(1)
                .isActive(true)
                .build();
        seat1.setId(UUID.randomUUID());

        Seat seat2 = Seat.builder()
                .bus(bus)
                .seatNumber("U2")
                .deck(Deck.LOWER)
                .seatType(SeatType.SLEEPER)
                .rowNum(1)
                .colNum(2)
                .isActive(true)
                .build();
        seat2.setId(UUID.randomUUID());

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(scheduleRepository.hasOverlappingSchedule(eq(bus.getId()), any(), eq(departure), eq(arrival))).thenReturn(false);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            s.setId(schedule.getId());
            return s;
        });
        when(seatRepository.findByBusIdAndIsActiveTrueOrderByRowNumAscColNumAsc(bus.getId())).thenReturn(List.of(seat1, seat2));

        ScheduleResponse response = scheduleService.createSchedule(request);

        assertNotNull(response);
        assertEquals(schedule.getId(), response.getId());
        assertEquals("DL-01-AB-1234", response.getBusRegistrationNumber());
        assertEquals("Delhi", response.getOrigin());
        assertEquals("Jaipur", response.getDestination());
        assertEquals(2L, response.getAvailableSeatsCount());

        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void shouldRejectScheduleWhenDepartureTimeAfterArrivalTime() {
        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus.getId())
                .routeId(route.getId())
                .scheduleDate(LocalDate.of(2026, 10, 15))
                .departureTime(LocalDateTime.of(2026, 10, 15, 18, 0))
                .arrivalTime(LocalDateTime.of(2026, 10, 15, 10, 0)) // before departure
                .build();

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));

        assertThrows(InvalidScheduleException.class, () -> scheduleService.createSchedule(request));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void shouldRejectScheduleWhenScheduleDateDoesNotMatchDepartureDate() {
        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus.getId())
                .routeId(route.getId())
                .scheduleDate(LocalDate.of(2026, 10, 16)) // Mismatch with departure
                .departureTime(LocalDateTime.of(2026, 10, 15, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 10, 15, 16, 0))
                .build();

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));

        assertThrows(InvalidScheduleException.class, () -> scheduleService.createSchedule(request));
    }

    @Test
    void shouldRejectScheduleWhenBusNotActive() {
        bus.setStatus(BusStatus.MAINTENANCE);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus.getId())
                .routeId(route.getId())
                .scheduleDate(LocalDate.of(2026, 10, 15))
                .departureTime(LocalDateTime.of(2026, 10, 15, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 10, 15, 16, 0))
                .build();

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));

        assertThrows(BusDomainException.class, () -> scheduleService.createSchedule(request));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void shouldRejectScheduleWhenRouteNotActive() {
        route.setIsActive(false);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus.getId())
                .routeId(route.getId())
                .scheduleDate(LocalDate.of(2026, 10, 15))
                .departureTime(LocalDateTime.of(2026, 10, 15, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 10, 15, 16, 0))
                .build();

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));

        assertThrows(BusDomainException.class, () -> scheduleService.createSchedule(request));
    }

    @Test
    void shouldRejectScheduleWhenOverlappingScheduleExists() {
        LocalDateTime departure = LocalDateTime.of(2026, 10, 15, 8, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 10, 15, 14, 0);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus.getId())
                .routeId(route.getId())
                .scheduleDate(LocalDate.of(2026, 10, 15))
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(scheduleRepository.hasOverlappingSchedule(eq(bus.getId()), any(), eq(departure), eq(arrival))).thenReturn(true);

        assertThrows(InvalidScheduleException.class, () -> scheduleService.createSchedule(request));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void shouldGetScheduleByIdSuccessfully() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleSeatInventoryRepository.countAvailableSeatsByScheduleId(schedule.getId())).thenReturn(30L);

        ScheduleResponse response = scheduleService.getScheduleById(schedule.getId());

        assertNotNull(response);
        assertEquals(schedule.getId(), response.getId());
        assertEquals(30L, response.getAvailableSeatsCount());
    }

    @Test
    void shouldCancelScheduleSuccessfully() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);
        when(scheduleSeatInventoryRepository.countAvailableSeatsByScheduleId(schedule.getId())).thenReturn(0L);

        ScheduleResponse response = scheduleService.cancelSchedule(schedule.getId(), "Heavy rain and route blocked");

        assertNotNull(response);
        assertEquals(ScheduleStatus.CANCELLED, schedule.getStatus());
        assertEquals(AvailabilityStatus.UNAVAILABLE, schedule.getAvailabilityStatus());
        assertEquals("Heavy rain and route blocked", schedule.getCancellationReason());
    }

    @Test
    void shouldGetTravellerScheduleDetailsSuccessfully() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(seatService.getScheduleSeatMap(schedule.getId())).thenReturn(
                ScheduleSeatMapResponse.builder()
                        .scheduleId(schedule.getId())
                        .busId(bus.getId())
                        .busNumber("BUS-001")
                        .registrationNumber("DL-01-AB-1234")
                        .totalSeats(2)
                        .availableSeatsCount(2L)
                        .build());
        when(fareService.getFaresByScheduleId(schedule.getId())).thenReturn(List.of(
                FareResponse.builder()
                        .seatType(SeatType.SLEEPER)
                        .baseAmount(java.math.BigDecimal.valueOf(500))
                        .totalAmount(java.math.BigDecimal.valueOf(525))
                        .currency("INR")
                        .build()));

        TravellerScheduleDetailResponse response = scheduleService.getTravellerScheduleDetails(schedule.getId());

        assertNotNull(response);
        assertEquals(schedule.getId(), response.getScheduleId());
        assertEquals("BUS-001", response.getBusNumber());
        assertEquals("DL-01-AB-1234", response.getRegistrationNumber());
        assertEquals("Express Lines", response.getOperatorCompanyName());
        assertEquals("Delhi", response.getOrigin());
        assertEquals("Jaipur", response.getDestination());
        assertNotNull(response.getSeatMap());
        assertEquals(2L, response.getSeatMap().getAvailableSeatsCount());
        assertEquals(1, response.getFares().size());
        assertNotNull(response.getCancellationPolicy());
    }
}
