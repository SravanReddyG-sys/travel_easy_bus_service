package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.CreateFareRequest;
import com.traveleasy.bus.dto.request.UpdateFareRequest;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.RouteRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.security.JwtTokenProvider;
import com.traveleasy.bus.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FareControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FareRepository fareRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private BusOperatorRepository operatorRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private UUID operatorId1;
    private String operatorToken1;
    private UUID operatorId2;
    private String operatorToken2;

    private Schedule schedule1;

    @BeforeEach
    void setUp() {
        fareRepository.deleteAll();
        scheduleRepository.deleteAll();
        busRepository.deleteAll();
        routeRepository.deleteAll();
        operatorRepository.deleteAll();

        // Operator 1
        BusOperator op1 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP1-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Fare Express 1")
                .contactEmail("op1@fares.com")
                .contactPhone("+911111111111")
                .build());
        operatorId1 = op1.getId();
        operatorToken1 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op1@fares.com", operatorId1, Set.of(UserRole.ROLE_OPERATOR));

        // Bus 1
        Bus bus1 = busRepository.save(Bus.builder()
                .operator(op1)
                .busNumber("BUS-F101")
                .registrationNumber("DL-01-FA-2000")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(30)
                .status(BusStatus.ACTIVE)
                .build());

        // Route 1
        Route route1 = routeRepository.save(Route.builder()
                .operator(op1)
                .routeCode("RT-DEL-JAI-F")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .build());

        // Schedule 1
        schedule1 = scheduleRepository.save(Schedule.builder()
                .bus(bus1)
                .route(route1)
                .operator(op1)
                .scheduleDate(LocalDate.of(2026, 12, 10))
                .departureTime(LocalDateTime.of(2026, 12, 10, 8, 0, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 10, 13, 0, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build());

        // Operator 2
        BusOperator op2 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Fare Express 2")
                .contactEmail("op2@fares.com")
                .contactPhone("+912222222222")
                .build());
        operatorId2 = op2.getId();
        operatorToken2 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op2@fares.com", operatorId2, Set.of(UserRole.ROLE_OPERATOR));
    }

    @Test
    void shouldConfigureAndRetrieveFaresSuccessfully() throws Exception {
        // 1. Configure Fare for SLEEPER
        CreateFareRequest sleeperFareReq = CreateFareRequest.builder()
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("900.00"))
                .taxAmount(new BigDecimal("45.00"))
                .discountAmount(new BigDecimal("20.00"))
                .currency("INR")
                .build();

        mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/fares")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sleeperFareReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seatType").value("SLEEPER"))
                .andExpect(jsonPath("$.data.baseAmount").value(900.00))
                .andExpect(jsonPath("$.data.taxAmount").value(45.00))
                .andExpect(jsonPath("$.data.discountAmount").value(20.00))
                .andExpect(jsonPath("$.data.totalAmount").value(925.00));

        // 2. Configure Bulk Fares (SEATER & SEMI_SLEEPER)
        CreateFareRequest seaterFareReq = CreateFareRequest.builder()
                .seatType(SeatType.SEATER)
                .baseAmount(new BigDecimal("500.00"))
                .taxAmount(new BigDecimal("25.00"))
                .discountAmount(BigDecimal.ZERO)
                .build();

        CreateFareRequest semiSleeperFareReq = CreateFareRequest.builder()
                .seatType(SeatType.SEMI_SLEEPER)
                .baseAmount(new BigDecimal("700.00"))
                .taxAmount(new BigDecimal("35.00"))
                .discountAmount(BigDecimal.ZERO)
                .build();

        mockMvc.perform(put("/api/v1/schedules/" + schedule1.getId() + "/fares/bulk")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(seaterFareReq, semiSleeperFareReq))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 3. Public GET all fares for the schedule
        mockMvc.perform(get("/api/v1/schedules/" + schedule1.getId() + "/fares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        // 4. Public GET fare for specific seat type
        mockMvc.perform(get("/api/v1/schedules/" + schedule1.getId() + "/fares/SLEEPER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seatType").value("SLEEPER"))
                .andExpect(jsonPath("$.data.totalAmount").value(925.00));
    }

    @Test
    void shouldUpdateFareSuccessfully() throws Exception {
        Fare fare = fareRepository.save(Fare.builder()
                .schedule(schedule1)
                .seatType(SeatType.WINDOW)
                .baseAmount(new BigDecimal("1200.00"))
                .taxAmount(new BigDecimal("60.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1260.00"))
                .currency("INR")
                .build());

        UpdateFareRequest updateReq = UpdateFareRequest.builder()
                .baseAmount(new BigDecimal("1100.00"))
                .taxAmount(new BigDecimal("55.00"))
                .discountAmount(new BigDecimal("100.00"))
                .currency("INR")
                .build();

        mockMvc.perform(put("/api/v1/schedules/" + schedule1.getId() + "/fares/" + fare.getId())
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseAmount").value(1100.00))
                .andExpect(jsonPath("$.data.totalAmount").value(1055.00));
    }

    @Test
    void shouldPreventUnauthorizedOperatorFromModifyingFares() throws Exception {
        CreateFareRequest request = CreateFareRequest.builder()
                .seatType(SeatType.SEATER)
                .baseAmount(new BigDecimal("400.00"))
                .build();

        // Operator 2 attempts to configure fare on Operator 1's schedule -> 403 Forbidden
        mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/fares")
                        .header("Authorization", operatorToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUS_FORBIDDEN"));
    }
}
