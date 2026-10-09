package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
import com.traveleasy.bus.repository.*;
import com.traveleasy.bus.security.JwtTokenProvider;
import com.traveleasy.bus.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperatorDashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusAvailabilityRepository busAvailabilityRepository;

    @Autowired
    private FareRepository fareRepository;

    @Autowired
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private BusOperatorRepository operatorRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private UUID operatorId;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        busAvailabilityRepository.deleteAll();
        fareRepository.deleteAll();
        inventoryRepository.deleteAll();
        scheduleRepository.deleteAll();
        seatRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        operatorRepository.deleteAll();

        // 1. Create Operator
        BusOperator operator = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP-DASH-INT")
                .companyName("Intercity Transit")
                .contactEmail("admin@transit.com")
                .contactPhone("9991112222")
                .isActive(true)
                .build());
        operatorId = operator.getId();
        operatorToken = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "admin@transit.com", operatorId, Set.of(UserRole.ROLE_OPERATOR));

        // 2. Create Bus
        Bus bus = busRepository.save(Bus.builder()
                .operator(operator)
                .busNumber("BUS-INT-01")
                .registrationNumber("DL-01-IT-9999")
                .busType(BusType.AC_SEATER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .build());

        Seat seat1 = seatRepository.save(Seat.builder().bus(bus).seatNumber("1A").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(1).colNum(1).build());
        Seat seat2 = seatRepository.save(Seat.builder().bus(bus).seatNumber("1B").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(1).colNum(2).build());

        // 3. Create Route
        Route route = routeRepository.save(Route.builder()
                .operator(operator)
                .routeCode("RT-INT-01")
                .origin("Delhi")
                .destination("Chandigarh")
                .distanceKm(250.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .build());

        // 4. Create Schedule
        Schedule schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.now())
                .departureTime(LocalDateTime.now().plusHours(2))
                .arrivalTime(LocalDateTime.now().plusHours(7))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .seatInventory(new ArrayList<>())
                .fares(new ArrayList<>())
                .build();

        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder().schedule(schedule).seat(seat1).status(SeatInventoryStatus.BOOKED).build();
        ScheduleSeatInventory inv2 = ScheduleSeatInventory.builder().schedule(schedule).seat(seat2).status(SeatInventoryStatus.AVAILABLE).build();
        schedule.getSeatInventory().add(inv1);
        schedule.getSeatInventory().add(inv2);

        Fare fare = Fare.builder().schedule(schedule).seatType(SeatType.SEATER).baseAmount(BigDecimal.valueOf(500)).totalAmount(BigDecimal.valueOf(550)).currency("INR").build();
        schedule.getFares().add(fare);

        scheduleRepository.save(schedule);
    }

    @Test
    @DisplayName("GET /api/v1/operator/dashboard/summary returns KPIs and summary")
    void getDashboardSummary_Success() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("Intercity Transit"))
                .andExpect(jsonPath("$.data.totalBuses").value(1))
                .andExpect(jsonPath("$.data.activeBuses").value(1))
                .andExpect(jsonPath("$.data.totalRoutes").value(1))
                .andExpect(jsonPath("$.data.totalSchedules").value(1))
                .andExpect(jsonPath("$.data.totalSeatsCapacity").value(2))
                .andExpect(jsonPath("$.data.totalSeatsBooked").value(1))
                .andExpect(jsonPath("$.data.occupancyRatePercentage").value(50.0))
                .andExpect(jsonPath("$.data.totalRevenue").value(550.0));
    }

    @Test
    @DisplayName("GET /api/v1/operator/dashboard/revenue returns route revenue breakdown")
    void getRevenueByRoute_Success() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/revenue")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].routeCode").value("RT-INT-01"))
                .andExpect(jsonPath("$.data[0].origin").value("Delhi"))
                .andExpect(jsonPath("$.data[0].destination").value("Chandigarh"))
                .andExpect(jsonPath("$.data[0].schedulesCount").value(1))
                .andExpect(jsonPath("$.data[0].totalBookedSeats").value(1))
                .andExpect(jsonPath("$.data[0].totalRevenue").value(550.0));
    }

    @Test
    @DisplayName("GET /api/v1/operator/dashboard/occupancy returns occupancy stats")
    void getOccupancy_Success() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/occupancy")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].busNumber").value("BUS-INT-01"))
                .andExpect(jsonPath("$.data[0].totalSeats").value(2))
                .andExpect(jsonPath("$.data[0].bookedSeats").value(1))
                .andExpect(jsonPath("$.data[0].availableSeats").value(1))
                .andExpect(jsonPath("$.data[0].occupancyPercentage").value(50.0));
    }

    @Test
    @DisplayName("GET /api/v1/operator/dashboard/schedules/{scheduleId}/manifest returns complete passenger manifest")
    void getScheduleManifest_Success() throws Exception {
        Schedule schedule = scheduleRepository.findAll().get(0);

        mockMvc.perform(get("/api/v1/operator/dashboard/schedules/" + schedule.getId() + "/manifest")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleId").value(schedule.getId().toString()))
                .andExpect(jsonPath("$.data.totalSeats").value(2))
                .andExpect(jsonPath("$.data.bookedSeatsCount").value(1))
                .andExpect(jsonPath("$.data.availableSeatsCount").value(1))
                .andExpect(jsonPath("$.data.seatRoster").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/operator/dashboard/feed/live-bookings returns real-time booking feed")
    void getLiveBookingFeed_Success() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/feed/live-bookings")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].seatNumber").value("1A"))
                .andExpect(jsonPath("$.data[0].status").value("BOOKED"));
    }

    @Test
    @DisplayName("Unauthenticated request should return 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
