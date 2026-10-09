package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.CreateScheduleRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleRequest;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Seat;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.BusRepository;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

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

    private Bus bus1;
    private Route route1;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        scheduleRepository.deleteAll();
        seatRepository.deleteAll();
        busRepository.deleteAll();
        routeRepository.deleteAll();
        operatorRepository.deleteAll();

        // Operator 1
        BusOperator op1 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP1-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Royal Travels")
                .contactEmail("op1@royal.com")
                .contactPhone("+911111111111")
                .build());
        operatorId1 = op1.getId();
        operatorToken1 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op1@royal.com", operatorId1, Set.of(UserRole.ROLE_OPERATOR));

        // Bus for Operator 1
        bus1 = busRepository.save(Bus.builder()
                .operator(op1)
                .busNumber("BUS-RYL-01")
                .registrationNumber("DL-01-XX-9999")
                .busType(BusType.AC_SEATER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .build());

        // Seats for Bus 1
        Seat seat1 = seatRepository.save(Seat.builder()
                .bus(bus1)
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .seatType(SeatType.SEATER)
                .rowNum(1)
                .colNum(1)
                .isActive(true)
                .build());

        Seat seat2 = seatRepository.save(Seat.builder()
                .bus(bus1)
                .seatNumber("1B")
                .deck(Deck.LOWER)
                .seatType(SeatType.SEATER)
                .rowNum(1)
                .colNum(2)
                .isActive(true)
                .build());

        // Route for Operator 1
        route1 = routeRepository.save(Route.builder()
                .operator(op1)
                .routeCode("RT-DEL-AGR-01")
                .origin("Delhi")
                .destination("Agra")
                .distanceKm(220.0)
                .estimatedDurationMinutes(240)
                .isActive(true)
                .build());

        // Operator 2
        BusOperator op2 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Star Travels")
                .contactEmail("op2@star.com")
                .contactPhone("+912222222222")
                .build());
        operatorId2 = op2.getId();
        operatorToken2 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op2@star.com", operatorId2, Set.of(UserRole.ROLE_OPERATOR));
    }

    @Test
    void shouldCreateScheduleAndAutoGenerateSeatInventory() throws Exception {
        LocalDate travelDate = LocalDate.of(2026, 11, 20);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 20, 7, 0, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 11, 20, 11, 0, 0);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus1.getId())
                .routeId(route1.getId())
                .scheduleDate(travelDate)
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        String responseContent = mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.busRegistrationNumber").value("DL-01-XX-9999"))
                .andExpect(jsonPath("$.data.origin").value("Delhi"))
                .andExpect(jsonPath("$.data.destination").value("Agra"))
                .andExpect(jsonPath("$.data.availableSeatsCount").value(2))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn().getResponse().getContentAsString();

        String scheduleIdStr = objectMapper.readTree(responseContent).path("data").path("id").asText();
        UUID scheduleId = UUID.fromString(scheduleIdStr);

        // Verify public get endpoint
        mockMvc.perform(get("/api/v1/schedules/" + scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(scheduleId.toString()))
                .andExpect(jsonPath("$.data.availableSeatsCount").value(2));
    }

    @Test
    void shouldRejectOverlappingScheduleOnSameBus() throws Exception {
        LocalDate travelDate = LocalDate.of(2026, 11, 20);
        LocalDateTime departure1 = LocalDateTime.of(2026, 11, 20, 7, 0, 0);
        LocalDateTime arrival1 = LocalDateTime.of(2026, 11, 20, 12, 0, 0);

        CreateScheduleRequest request1 = CreateScheduleRequest.builder()
                .busId(bus1.getId())
                .routeId(route1.getId())
                .scheduleDate(travelDate)
                .departureTime(departure1)
                .arrivalTime(arrival1)
                .build();

        // 1. First schedule succeeds
        mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // 2. Overlapping schedule (09:00 to 14:00) should be rejected
        LocalDateTime departure2 = LocalDateTime.of(2026, 11, 20, 9, 0, 0);
        LocalDateTime arrival2 = LocalDateTime.of(2026, 11, 20, 14, 0, 0);

        CreateScheduleRequest request2 = CreateScheduleRequest.builder()
                .busId(bus1.getId())
                .routeId(route1.getId())
                .scheduleDate(travelDate)
                .departureTime(departure2)
                .arrivalTime(arrival2)
                .build();

        mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUS_SCHEDULE_INVALID"));
    }

    @Test
    void shouldCancelScheduleSuccessfully() throws Exception {
        LocalDate travelDate = LocalDate.of(2026, 11, 20);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 20, 7, 0, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 11, 20, 11, 0, 0);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus1.getId())
                .routeId(route1.getId())
                .scheduleDate(travelDate)
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        String responseContent = mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String scheduleIdStr = objectMapper.readTree(responseContent).path("data").path("id").asText();
        UUID scheduleId = UUID.fromString(scheduleIdStr);

        mockMvc.perform(patch("/api/v1/schedules/" + scheduleId + "/cancel")
                        .header("Authorization", operatorToken1)
                        .param("reason", "Foggy weather conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.availabilityStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Foggy weather conditions"));
    }

    @Test
    void shouldPreventUnauthorizedOperatorFromUpdatingSchedule() throws Exception {
        LocalDate travelDate = LocalDate.of(2026, 11, 20);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 20, 7, 0, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 11, 20, 11, 0, 0);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus1.getId())
                .routeId(route1.getId())
                .scheduleDate(travelDate)
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        String responseContent = mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String scheduleIdStr = objectMapper.readTree(responseContent).path("data").path("id").asText();
        UUID scheduleId = UUID.fromString(scheduleIdStr);

        // Operator 2 attempts to cancel Operator 1's schedule -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/schedules/" + scheduleId + "/cancel")
                        .header("Authorization", operatorToken2)
                        .param("reason", "Malicious cancellation attempt"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUS_FORBIDDEN"));
    }

    @Test
    void shouldGetFullTravellerScheduleDetailsSuccessfully() throws Exception {
        LocalDate travelDate = LocalDate.of(2026, 11, 20);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 20, 7, 0, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 11, 20, 11, 0, 0);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .busId(bus1.getId())
                .routeId(route1.getId())
                .scheduleDate(travelDate)
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();

        String responseContent = mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String scheduleIdStr = objectMapper.readTree(responseContent).path("data").path("id").asText();
        UUID scheduleId = UUID.fromString(scheduleIdStr);

        // Fetch full traveller details (Public endpoint)
        mockMvc.perform(get("/api/v1/schedules/" + scheduleId + "/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleId").value(scheduleId.toString()))
                .andExpect(jsonPath("$.data.busNumber").value("BUS-RYL-01"))
                .andExpect(jsonPath("$.data.registrationNumber").value("DL-01-XX-9999"))
                .andExpect(jsonPath("$.data.operatorCompanyName").value("Royal Travels"))
                .andExpect(jsonPath("$.data.origin").value("Delhi"))
                .andExpect(jsonPath("$.data.destination").value("Agra"))
                .andExpect(jsonPath("$.data.seatMap.availableSeatsCount").value(2))
                .andExpect(jsonPath("$.data.seatMap.lowerDeckSeats.length()").value(2))
                .andExpect(jsonPath("$.data.cancellationPolicy").isNotEmpty());
    }
}
