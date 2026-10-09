package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.BoardingPointRequest;
import com.traveleasy.bus.dto.request.CreateRouteRequest;
import com.traveleasy.bus.dto.request.DroppingPointRequest;
import com.traveleasy.bus.dto.request.RouteStopRequest;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.RouteRepository;
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

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.traveleasy.bus.repository.FareRepository fareRepository;

    @Autowired
    private com.traveleasy.bus.repository.ScheduleSeatInventoryRepository inventoryRepository;

    @Autowired
    private com.traveleasy.bus.repository.ScheduleRepository scheduleRepository;

    @Autowired
    private com.traveleasy.bus.repository.SeatRepository seatRepository;

    @Autowired
    private com.traveleasy.bus.repository.BusRepository busRepository;

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

    @BeforeEach
    void setUp() {
        fareRepository.deleteAll();
        inventoryRepository.deleteAll();
        scheduleRepository.deleteAll();
        seatRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        operatorRepository.deleteAll();

        BusOperator op1 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP1-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Route Operator 1")
                .contactEmail("op1@routes.com")
                .contactPhone("+911111111111")
                .build());
        operatorId1 = op1.getId();
        operatorToken1 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op1@routes.com", operatorId1, Set.of(UserRole.ROLE_OPERATOR));

        BusOperator op2 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Route Operator 2")
                .contactEmail("op2@routes.com")
                .contactPhone("+912222222222")
                .build());
        operatorId2 = op2.getId();
        operatorToken2 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op2@routes.com", operatorId2, Set.of(UserRole.ROLE_OPERATOR));
    }

    @Test
    void shouldCreateRouteAndAddStopsAndBoardingDroppingPoints() throws Exception {
        // 1. Create Route
        CreateRouteRequest createRouteRequest = CreateRouteRequest.builder()
                .routeCode("RT-DEL-JAI-EXP")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(330)
                .build();

        String routeResponseContent = mockMvc.perform(post("/api/v1/routes")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRouteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.routeCode").value("RT-DEL-JAI-EXP"))
                .andExpect(jsonPath("$.data.origin").value("Delhi"))
                .andExpect(jsonPath("$.data.destination").value("Jaipur"))
                .andReturn().getResponse().getContentAsString();

        String routeIdStr = objectMapper.readTree(routeResponseContent).path("data").path("id").asText();
        UUID routeId = UUID.fromString(routeIdStr);

        // 2. Add Stop
        RouteStopRequest stopRequest = RouteStopRequest.builder()
                .stopName("Gurgaon IFFCO Chowk")
                .stopOrder(1)
                .distanceFromOriginKm(35.0)
                .build();

        mockMvc.perform(post("/api/v1/routes/" + routeId + "/stops")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stopRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stopName").value("Gurgaon IFFCO Chowk"))
                .andExpect(jsonPath("$.data.stopOrder").value(1));

        // 3. Add Boarding Point
        BoardingPointRequest boardingRequest = BoardingPointRequest.builder()
                .locationName("Kashmere Gate ISBT")
                .landmark("Gate 1")
                .departureOffsetMinutes(0)
                .contactPhone("+919876543210")
                .build();

        mockMvc.perform(post("/api/v1/routes/" + routeId + "/boarding-points")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.locationName").value("Kashmere Gate ISBT"));

        // 4. Add Dropping Point
        DroppingPointRequest droppingRequest = DroppingPointRequest.builder()
                .locationName("Sindhi Camp Bus Stand")
                .landmark("Platform 3")
                .arrivalOffsetMinutes(330)
                .contactPhone("+919876543210")
                .build();

        mockMvc.perform(post("/api/v1/routes/" + routeId + "/dropping-points")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(droppingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.locationName").value("Sindhi Camp Bus Stand"));

        // 5. Get complete Route with stops and points
        mockMvc.perform(get("/api/v1/routes/" + routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stops").isArray())
                .andExpect(jsonPath("$.data.stops.length()").value(1))
                .andExpect(jsonPath("$.data.boardingPoints.length()").value(1))
                .andExpect(jsonPath("$.data.droppingPoints.length()").value(1));
    }

    @Test
    void shouldRejectRouteCreationWhenOriginEqualsDestination() throws Exception {
        CreateRouteRequest request = CreateRouteRequest.builder()
                .routeCode("RT-IDENTICAL")
                .origin("Delhi")
                .destination("DELHI")
                .distanceKm(50.0)
                .estimatedDurationMinutes(60)
                .build();

        mockMvc.perform(post("/api/v1/routes")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUS_ROUTE_INVALID"));
    }

    @Test
    void shouldPreventOperatorFromMutatingAnotherOperatorsRoute() throws Exception {
        BusOperator op1 = operatorRepository.findById(operatorId1).orElseThrow();
        Route route = routeRepository.save(Route.builder()
                .operator(op1)
                .routeCode("RT-OP1-EXCLUSIVE")
                .origin("Mumbai")
                .destination("Pune")
                .distanceKm(150.0)
                .estimatedDurationMinutes(180)
                .build());

        RouteStopRequest stopRequest = RouteStopRequest.builder()
                .stopName("Lonavala")
                .stopOrder(1)
                .distanceFromOriginKm(80.0)
                .build();

        // Operator 2 attempts to add a stop to Operator 1's route -> 403 Forbidden
        mockMvc.perform(post("/api/v1/routes/" + route.getId() + "/stops")
                        .header("Authorization", operatorToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stopRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUS_FORBIDDEN"));
    }
}
