package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.CreateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusRequest;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.BusRepository;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusControllerIntegrationTest {

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
    private com.traveleasy.bus.repository.RouteRepository routeRepository;

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
    private String travellerToken;

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
                .companyName("First Operator Co")
                .contactEmail("op1@test.com")
                .contactPhone("+911111111111")
                .build());
        operatorId1 = op1.getId();
        operatorToken1 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op1@test.com", operatorId1, Set.of(UserRole.ROLE_OPERATOR));

        BusOperator op2 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName("Second Operator Co")
                .contactEmail("op2@test.com")
                .contactPhone("+912222222222")
                .build());
        operatorId2 = op2.getId();
        operatorToken2 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op2@test.com", operatorId2, Set.of(UserRole.ROLE_OPERATOR));

        travellerToken = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "traveller@test.com", null, Set.of(UserRole.ROLE_TRAVELLER));
    }

    @Test
    void shouldCreateBusSuccessfullyWithOperatorRole() throws Exception {
        CreateBusRequest request = CreateBusRequest.builder()
                .busNumber("BUS-INT-101")
                .registrationNumber("MH-02-CD-4567")
                .busType(BusType.VOLVO_MULTI_AXLE)
                .totalSeats(45)
                .amenities(List.of("WiFi", "Recliner Seats"))
                .build();

        mockMvc.perform(post("/api/v1/buses")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.busNumber").value("BUS-INT-101"))
                .andExpect(jsonPath("$.data.registrationNumber").value("MH-02-CD-4567"))
                .andExpect(jsonPath("$.data.operatorId").value(operatorId1.toString()));
    }

    @Test
    void shouldRejectBusCreationWithMissingRequiredFields() throws Exception {
        CreateBusRequest invalidRequest = CreateBusRequest.builder()
                .busNumber("") // blank bus number
                .totalSeats(-5) // invalid seats
                .build();

        mockMvc.perform(post("/api/v1/buses")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUS_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldRejectBusCreationWhenUnauthenticated() throws Exception {
        CreateBusRequest request = CreateBusRequest.builder()
                .busNumber("BUS-UNAUTH")
                .registrationNumber("KA-05-ZZ-1111")
                .busType(BusType.AC_SEATER)
                .totalSeats(30)
                .build();

        mockMvc.perform(post("/api/v1/buses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BUS_UNAUTHORIZED"));
    }

    @Test
    void shouldRejectBusCreationByTravellerRole() throws Exception {
        CreateBusRequest request = CreateBusRequest.builder()
                .busNumber("BUS-TRAVELLER")
                .registrationNumber("KA-05-ZZ-2222")
                .busType(BusType.AC_SEATER)
                .totalSeats(30)
                .build();

        mockMvc.perform(post("/api/v1/buses")
                        .header("Authorization", travellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventOperatorFromModifyingAnotherOperatorsBus() throws Exception {
        // Create bus owned by Operator 1
        BusOperator op1 = operatorRepository.findById(operatorId1).orElseThrow();
        Bus bus = busRepository.save(Bus.builder()
                .operator(op1)
                .busNumber("BUS-OP1-EXCLUSIVE")
                .registrationNumber("DL-09-OP-0001")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(36)
                .build());

        UpdateBusRequest updateRequest = UpdateBusRequest.builder()
                .busNumber("BUS-OP1-TAMPERED")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(36)
                .status(BusStatus.ACTIVE)
                .build();

        // Operator 2 attempts to modify Operator 1's bus -> Must be rejected with 403 Forbidden
        mockMvc.perform(put("/api/v1/buses/" + bus.getId())
                        .header("Authorization", operatorToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUS_FORBIDDEN"));
    }

    @Test
    void shouldGetBusByIdAndListBuses() throws Exception {
        BusOperator op1 = operatorRepository.findById(operatorId1).orElseThrow();
        Bus bus = busRepository.save(Bus.builder()
                .operator(op1)
                .busNumber("BUS-QUERY-101")
                .registrationNumber("RJ-14-AA-9999")
                .busType(BusType.ELECTRIC_AC)
                .totalSeats(50)
                .build());

        // Get by ID
        mockMvc.perform(get("/api/v1/buses/" + bus.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.busNumber").value("BUS-QUERY-101"))
                .andExpect(jsonPath("$.data.busType").value("ELECTRIC_AC"));

        // List buses
        mockMvc.perform(get("/api/v1/buses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
