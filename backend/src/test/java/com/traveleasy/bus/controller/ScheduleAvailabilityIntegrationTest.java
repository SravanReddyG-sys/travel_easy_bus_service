package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.UpdateScheduleAvailabilityRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleStatusRequest;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.repository.BusAvailabilityRepository;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.RouteRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.repository.SeatRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleAvailabilityIntegrationTest {

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

    private UUID operatorId1;
    private String operatorToken1;
    private UUID operatorId2;
    private String operatorToken2;

    private Schedule schedule1;

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

        // Operator 1
        BusOperator op1 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP1-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Availability Operator 1")
                .contactEmail("op1@avail.com")
                .contactPhone("+911111111111")
                .build());
        operatorId1 = op1.getId();
        operatorToken1 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op1@avail.com", operatorId1, Set.of(UserRole.ROLE_OPERATOR));

        // Bus 1
        Bus bus1 = busRepository.save(Bus.builder()
                .operator(op1)
                .busNumber("BUS-AV-101")
                .registrationNumber("DL-01-AV-1111")
                .busType(BusType.AC_SEATER)
                .totalSeats(30)
                .status(BusStatus.ACTIVE)
                .build());

        // Route 1
        Route route1 = routeRepository.save(Route.builder()
                .operator(op1)
                .routeCode("RT-DEL-HAR")
                .origin("Delhi")
                .destination("Haridwar")
                .distanceKm(210.0)
                .estimatedDurationMinutes(260)
                .isActive(true)
                .build());

        // Schedule 1
        schedule1 = scheduleRepository.save(Schedule.builder()
                .bus(bus1)
                .route(route1)
                .operator(op1)
                .scheduleDate(LocalDate.of(2026, 12, 25))
                .departureTime(LocalDateTime.of(2026, 12, 25, 7, 0, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 25, 12, 0, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build());

        // Operator 2
        BusOperator op2 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Availability Operator 2")
                .contactEmail("op2@avail.com")
                .contactPhone("+912222222222")
                .build());
        operatorId2 = op2.getId();
        operatorToken2 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op2@avail.com", operatorId2, Set.of(UserRole.ROLE_OPERATOR));
    }

    @Test
    void shouldToggleAvailabilityAndRecordAuditHistory() throws Exception {
        UpdateScheduleAvailabilityRequest request = UpdateScheduleAvailabilityRequest.builder()
                .availabilityStatus(AvailabilityStatus.UNAVAILABLE)
                .reason("Heavy rainfall alert")
                .build();

        // 1. Toggle availability to UNAVAILABLE
        mockMvc.perform(put("/api/v1/schedules/" + schedule1.getId() + "/availability")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availabilityStatus").value("UNAVAILABLE"));

        // 2. Fetch audit history
        mockMvc.perform(get("/api/v1/schedules/" + schedule1.getId() + "/availability/history")
                        .header("Authorization", operatorToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data[0].reason").value("Heavy rainfall alert"))
                .andExpect(jsonPath("$.data[0].modifiedBy").value("op1@avail.com"));
    }

    @Test
    void shouldTransitionScheduleStatusStepByStep() throws Exception {
        // 1. SCHEDULED -> BOARDING
        UpdateScheduleStatusRequest boardingReq = UpdateScheduleStatusRequest.builder()
                .status(ScheduleStatus.BOARDING)
                .reason("Gate open")
                .build();

        mockMvc.perform(patch("/api/v1/schedules/" + schedule1.getId() + "/status")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardingReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BOARDING"));

        // 2. BOARDING -> DEPARTED
        UpdateScheduleStatusRequest departedReq = UpdateScheduleStatusRequest.builder()
                .status(ScheduleStatus.DEPARTED)
                .build();

        mockMvc.perform(patch("/api/v1/schedules/" + schedule1.getId() + "/status")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(departedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEPARTED"));

        // 3. DEPARTED -> COMPLETED
        UpdateScheduleStatusRequest completedReq = UpdateScheduleStatusRequest.builder()
                .status(ScheduleStatus.COMPLETED)
                .build();

        mockMvc.perform(patch("/api/v1/schedules/" + schedule1.getId() + "/status")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.availabilityStatus").value("UNAVAILABLE"));
    }

    @Test
    void shouldPreventUnauthorizedOperatorFromModifyingAvailability() throws Exception {
        UpdateScheduleAvailabilityRequest request = UpdateScheduleAvailabilityRequest.builder()
                .availabilityStatus(AvailabilityStatus.UNAVAILABLE)
                .build();

        // Operator 2 attempts to change Operator 1's schedule availability -> 403 Forbidden
        mockMvc.perform(put("/api/v1/schedules/" + schedule1.getId() + "/availability")
                        .header("Authorization", operatorToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUS_FORBIDDEN"));
    }
}
