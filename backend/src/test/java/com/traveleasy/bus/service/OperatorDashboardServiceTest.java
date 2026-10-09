package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.response.OperatorDashboardSummaryResponse;
import com.traveleasy.bus.dto.response.OperatorOccupancyStatResponse;
import com.traveleasy.bus.dto.response.OperatorRevenueStatResponse;
import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
import com.traveleasy.bus.mapper.ScheduleMapper;
import com.traveleasy.bus.repository.*;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.impl.OperatorDashboardServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorDashboardServiceTest {

    @Mock
    private BusOperatorRepository operatorRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Mock
    private FareRepository fareRepository;

    @Spy
    private ScheduleMapper scheduleMapper = new ScheduleMapper();

    @InjectMocks
    private OperatorDashboardServiceImpl dashboardService;

    private UUID operatorId;
    private BusOperator operator;
    private Bus bus1;
    private Route route1;
    private Schedule schedule1;
    private Seat seat1;
    private Seat seat2;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        operator = BusOperator.builder()
                .operatorCode("OP-DASH-01")
                .companyName("Metro Express")
                .build();
        operator.setId(operatorId);

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("op@metro.com")
                .operatorId(operatorId)
                .roles(Set.of(UserRole.ROLE_OPERATOR))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        bus1 = Bus.builder()
                .operator(operator)
                .busNumber("BUS-DASH-01")
                .registrationNumber("DL-01-DS-1111")
                .busType(BusType.AC_SEATER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .build();
        bus1.setId(UUID.randomUUID());

        route1 = Route.builder()
                .operator(operator)
                .routeCode("RT-DASH-01")
                .origin("Delhi")
                .destination("Agra")
                .distanceKm(200.0)
                .estimatedDurationMinutes(240)
                .isActive(true)
                .build();
        route1.setId(UUID.randomUUID());

        schedule1 = Schedule.builder()
                .bus(bus1)
                .route(route1)
                .operator(operator)
                .scheduleDate(LocalDate.now())
                .departureTime(LocalDateTime.now().plusHours(2))
                .arrivalTime(LocalDateTime.now().plusHours(6))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .fares(new ArrayList<>())
                .build();
        schedule1.setId(UUID.randomUUID());

        Fare fare = Fare.builder()
                .schedule(schedule1)
                .seatType(SeatType.SEATER)
                .baseAmount(BigDecimal.valueOf(400))
                .totalAmount(BigDecimal.valueOf(450))
                .currency("INR")
                .build();
        schedule1.getFares().add(fare);

        seat1 = Seat.builder().bus(bus1).seatNumber("1A").seatType(SeatType.SEATER).build();
        seat2 = Seat.builder().bus(bus1).seatNumber("1B").seatType(SeatType.SEATER).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should generate dashboard summary with accurate KPIs and financial metrics")
    void getDashboardSummary_Success() {
        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat1).status(SeatInventoryStatus.BOOKED).build();
        ScheduleSeatInventory inv2 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat2).status(SeatInventoryStatus.AVAILABLE).build();

        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(busRepository.findByOperatorId(operatorId)).thenReturn(List.of(bus1));
        when(routeRepository.findByOperatorId(operatorId)).thenReturn(List.of(route1));
        when(scheduleRepository.findByOperatorId(operatorId)).thenReturn(List.of(schedule1));
        when(inventoryRepository.findByScheduleIdWithSeats(schedule1.getId())).thenReturn(List.of(inv1, inv2));
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule1.getId())).thenReturn(1L);

        OperatorDashboardSummaryResponse response = dashboardService.getDashboardSummary(operatorId);

        assertThat(response).isNotNull();
        assertThat(response.getCompanyName()).isEqualTo("Metro Express");
        assertThat(response.getTotalBuses()).isEqualTo(1);
        assertThat(response.getActiveBuses()).isEqualTo(1);
        assertThat(response.getTotalRoutes()).isEqualTo(1);
        assertThat(response.getActiveRoutes()).isEqualTo(1);
        assertThat(response.getTotalSchedules()).isEqualTo(1);
        assertThat(response.getTodaySchedules()).isEqualTo(1);
        assertThat(response.getTotalSeatsCapacity()).isEqualTo(2);
        assertThat(response.getTotalSeatsBooked()).isEqualTo(1);
        assertThat(response.getOccupancyRatePercentage()).isEqualTo(50.0);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("Should calculate revenue by route")
    void getRevenueByRoute_Success() {
        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat1).status(SeatInventoryStatus.BOOKED).build();

        when(routeRepository.findByOperatorId(operatorId)).thenReturn(List.of(route1));
        when(scheduleRepository.findByOperatorId(operatorId)).thenReturn(List.of(schedule1));
        when(inventoryRepository.findByScheduleIdWithSeats(schedule1.getId())).thenReturn(List.of(inv1));

        List<OperatorRevenueStatResponse> result = dashboardService.getRevenueByRoute(operatorId);

        assertThat(result).hasSize(1);
        OperatorRevenueStatResponse stat = result.get(0);
        assertThat(stat.getRouteCode()).isEqualTo("RT-DASH-01");
        assertThat(stat.getSchedulesCount()).isEqualTo(1);
        assertThat(stat.getTotalBookedSeats()).isEqualTo(1);
        assertThat(stat.getTotalRevenue()).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("Should calculate trip occupancy analytics")
    void getOccupancyAnalytics_Success() {
        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat1).status(SeatInventoryStatus.BOOKED).build();
        ScheduleSeatInventory inv2 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat2).status(SeatInventoryStatus.AVAILABLE).build();

        when(scheduleRepository.findByOperatorId(operatorId)).thenReturn(List.of(schedule1));
        when(inventoryRepository.findByScheduleIdWithSeats(schedule1.getId())).thenReturn(List.of(inv1, inv2));

        List<OperatorOccupancyStatResponse> result = dashboardService.getOccupancyAnalytics(operatorId, null, null);

        assertThat(result).hasSize(1);
        OperatorOccupancyStatResponse stat = result.get(0);
        assertThat(stat.getBusNumber()).isEqualTo("BUS-DASH-01");
        assertThat(stat.getTotalSeats()).isEqualTo(2);
        assertThat(stat.getBookedSeats()).isEqualTo(1);
        assertThat(stat.getAvailableSeats()).isEqualTo(1);
        assertThat(stat.getOccupancyPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should retrieve complete schedule passenger manifest and seat roster")
    void getScheduleManifest_Success() {
        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat1).status(SeatInventoryStatus.BOOKED).bookingReference("BKG-101").build();
        ScheduleSeatInventory inv2 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat2).status(SeatInventoryStatus.AVAILABLE).build();

        when(scheduleRepository.findById(schedule1.getId())).thenReturn(Optional.of(schedule1));
        when(inventoryRepository.findByScheduleIdWithSeats(schedule1.getId())).thenReturn(List.of(inv1, inv2));

        var response = dashboardService.getScheduleManifest(schedule1.getId());

        assertThat(response).isNotNull();
        assertThat(response.getScheduleId()).isEqualTo(schedule1.getId());
        assertThat(response.getTotalSeats()).isEqualTo(2);
        assertThat(response.getBookedSeatsCount()).isEqualTo(1);
        assertThat(response.getAvailableSeatsCount()).isEqualTo(1);
        assertThat(response.getOccupancyPercentage()).isEqualTo(50.0);
        assertThat(response.getGrossRevenue()).isEqualByComparingTo("450.00");
        assertThat(response.getSeatRoster()).hasSize(2);
        assertThat(response.getSeatRoster().get(0).getBookingReference()).isEqualTo("BKG-101");
    }

    @Test
    @DisplayName("Should retrieve real-time live booking feed for operator")
    void getLiveBookingFeed_Success() {
        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder().schedule(schedule1).seat(seat1).status(SeatInventoryStatus.BOOKED).bookingReference("BKG-101").build();

        when(scheduleRepository.findByOperatorId(operatorId)).thenReturn(List.of(schedule1));
        when(inventoryRepository.findByScheduleIdWithSeats(schedule1.getId())).thenReturn(List.of(inv1));

        var feed = dashboardService.getLiveBookingFeed(operatorId, 10);

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).getSeatNumber()).isEqualTo("1A");
        assertThat(feed.get(0).getBookingReference()).isEqualTo("BKG-101");
        assertThat(feed.get(0).getStatus()).isEqualTo(SeatInventoryStatus.BOOKED);
    }
}
