package com.traveleasy.bus.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.*;
import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
import com.traveleasy.bus.integration.booking.dto.BookingSeatCancelRequest;
import com.traveleasy.bus.integration.booking.dto.BookingSeatConfirmRequest;
import com.traveleasy.bus.integration.booking.dto.BookingSeatHoldRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusMicroserviceE2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusOperatorRepository operatorRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Autowired
    private FareRepository fareRepository;

    @Autowired
    private BusEventOutboxRepository outboxRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private UUID operatorId;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        inventoryRepository.deleteAll();
        fareRepository.deleteAll();
        scheduleRepository.deleteAll();
        seatRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        operatorRepository.deleteAll();

        // Register default operator
        BusOperator operator = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP-E2E")
                .companyName("Royal Star Travels")
                .contactEmail("contact@royalstar.com")
                .contactPhone("9876543210")
                .isActive(true)
                .build());
        operatorId = operator.getId();
        operatorToken = "Bearer " + tokenProvider.generateToken(
                UUID.randomUUID(), "contact@royalstar.com", operatorId, Set.of(UserRole.ROLE_OPERATOR));
    }

    @Test
    @DisplayName("Complete E2E Flow: Bus Creation -> Route -> Schedule & Auto-Inventory -> Fare -> Search -> Seat Lock -> Booking Confirm -> Cancel & Outbox Event")
    void executeFullE2ELifecycleFlow() throws Exception {
        // Step 1: Operator creates a Bus
        CreateBusRequest createBusReq = CreateBusRequest.builder()
                .busNumber("RS-E2E-100")
                .registrationNumber("DL-01-RS-7777")
                .busType(BusType.AC_SEATER)
                .totalSeats(4)
                .amenities(List.of("WiFi", "Charging Point", "Water Bottle"))
                .build();

        MvcResult busResult = mockMvc.perform(post("/api/v1/buses")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBusReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String busIdStr = objectMapper.readTree(busResult.getResponse().getContentAsString())
                .get("data").get("id").asText();
        UUID busId = UUID.fromString(busIdStr);

        // Configure Physical Seats for Bus
        BulkSeatConfigRequest configureSeatsReq = BulkSeatConfigRequest.builder()
                .seats(List.of(
                        CreateSeatRequest.builder().seatNumber("1A").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(1).colNum(1).build(),
                        CreateSeatRequest.builder().seatNumber("1B").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(1).colNum(2).build(),
                        CreateSeatRequest.builder().seatNumber("2A").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(2).colNum(1).build(),
                        CreateSeatRequest.builder().seatNumber("2B").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(2).colNum(2).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/buses/" + busId + "/seats")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configureSeatsReq)))
                .andExpect(status().isCreated());

        // Step 2: Operator creates a Route
        CreateRouteRequest createRouteReq = CreateRouteRequest.builder()
                .routeCode("RT-DEL-JAI-E2E")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(330)
                .build();

        MvcResult routeResult = mockMvc.perform(post("/api/v1/routes")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRouteReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String routeIdStr = objectMapper.readTree(routeResult.getResponse().getContentAsString())
                .get("data").get("id").asText();
        UUID routeId = UUID.fromString(routeIdStr);

        // Step 3: Operator creates a Schedule (Auto-initializes inventory for 4 seats)
        LocalDate travelDate = LocalDate.now().plusDays(2);
        LocalDateTime departureTime = travelDate.atTime(8, 0);
        LocalDateTime arrivalTime = travelDate.atTime(13, 30);

        CreateScheduleRequest createScheduleReq = CreateScheduleRequest.builder()
                .busId(busId)
                .routeId(routeId)
                .scheduleDate(travelDate)
                .departureTime(departureTime)
                .arrivalTime(arrivalTime)
                .build();

        MvcResult scheduleResult = mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createScheduleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.availableSeatsCount").value(4))
                .andReturn();

        String scheduleIdStr = objectMapper.readTree(scheduleResult.getResponse().getContentAsString())
                .get("data").get("id").asText();
        UUID scheduleId = UUID.fromString(scheduleIdStr);

        // Step 4: Operator configures Fares for the Schedule
        CreateFareRequest fareReq = CreateFareRequest.builder()
                .seatType(SeatType.SEATER)
                .baseAmount(BigDecimal.valueOf(500.00))
                .taxAmount(BigDecimal.valueOf(50.00))
                .discountAmount(BigDecimal.ZERO)
                .currency("INR")
                .build();

        mockMvc.perform(post("/api/v1/schedules/" + scheduleId + "/fares")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fareReq)))
                .andExpect(status().isCreated());

        // Step 5: Traveller searches for Buses
        mockMvc.perform(get("/api/v1/buses/search")
                        .param("origin", "Delhi")
                        .param("destination", "Jaipur")
                        .param("travelDate", travelDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].availableSeatsCount").value(4))
                .andExpect(jsonPath("$.data.content[0].startingFare").value(550.0));

        // Step 6: Traveller views Schedule Details & Seat Map
        mockMvc.perform(get("/api/v1/schedules/" + scheduleId + "/details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seatMap.availableSeatsCount").value(4));

        // Step 7: Central Booking Integration holds/locks 2 seats ("1A", "1B")
        BookingSeatHoldRequest holdReq = BookingSeatHoldRequest.builder()
                .scheduleId(scheduleId)
                .seatNumbers(List.of("1A", "1B"))
                .holdDurationMinutes(10)
                .build();

        MvcResult holdResult = mockMvc.perform(post("/api/v1/integration/booking/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockedSeats").isArray())
                .andExpect(jsonPath("$.data.totalHoldAmount").value(1100.0))
                .andReturn();

        String lockToken = objectMapper.readTree(holdResult.getResponse().getContentAsString())
                .get("data").get("lockToken").asText();

        // Step 8: Central Booking Integration confirms seats with booking reference
        BookingSeatConfirmRequest confirmReq = BookingSeatConfirmRequest.builder()
                .scheduleId(scheduleId)
                .seatNumbers(List.of("1A", "1B"))
                .lockToken(lockToken)
                .bookingReference("BKG-E2E-9999")
                .build();

        mockMvc.perform(post("/api/v1/integration/booking/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("BKG-E2E-9999"))
                .andExpect(jsonPath("$.data.totalAmount").value(1100.0));

        // Step 9: Verify Operator Dashboard Summary and Manifest
        mockMvc.perform(get("/api/v1/operator/dashboard/summary")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSeatsBooked").value(2))
                .andExpect(jsonPath("$.data.totalSeatsCapacity").value(4))
                .andExpect(jsonPath("$.data.occupancyRatePercentage").value(50.0))
                .andExpect(jsonPath("$.data.totalRevenue").value(1100.0));

        mockMvc.perform(get("/api/v1/operator/dashboard/schedules/" + scheduleId + "/manifest")
                        .header("Authorization", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookedSeatsCount").value(2))
                .andExpect(jsonPath("$.data.availableSeatsCount").value(2));

        // Step 10: Central Booking cancels booking; seats released
        BookingSeatCancelRequest cancelReq = BookingSeatCancelRequest.builder()
                .scheduleId(scheduleId)
                .bookingReference("BKG-E2E-9999")
                .cancellationReason("Traveller plan change")
                .build();

        mockMvc.perform(post("/api/v1/integration/booking/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelled").value(true))
                .andExpect(jsonPath("$.data.releasedSeats").isArray());

        // Step 11: Verify Outbox Table has recorded domain events
        var outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents).isNotEmpty();
    }
}
