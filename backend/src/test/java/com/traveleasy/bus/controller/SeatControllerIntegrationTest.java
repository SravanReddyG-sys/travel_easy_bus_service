package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.BlockSeatRequest;
import com.traveleasy.bus.dto.request.BulkSeatConfigRequest;
import com.traveleasy.bus.dto.request.CreateSeatRequest;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.entity.ScheduleSeatInventory;
import com.traveleasy.bus.entity.Seat;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.enums.SeatType;
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
class SeatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private Bus bus1;
    private Schedule schedule1;
    private Seat seat1A;
    private Seat seat1B;

    @BeforeEach
    void setUp() {
        fareRepository.deleteAll();
        inventoryRepository.deleteAll();
        scheduleRepository.deleteAll();
        seatRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        operatorRepository.deleteAll();

        // Operator 1
        BusOperator op1 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP-SEAT-INT")
                .companyName("Seat Express")
                .contactEmail("op1@seats.com")
                .contactPhone("+911111111111")
                .build());
        operatorId1 = op1.getId();
        operatorToken1 = "Bearer " + tokenProvider.generateToken(UUID.randomUUID(), "op1@seats.com", operatorId1, Set.of(UserRole.ROLE_OPERATOR));

        // Bus 1
        bus1 = busRepository.save(Bus.builder()
                .operator(op1)
                .busNumber("BUS-S-101")
                .registrationNumber("DL-01-SE-5555")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .build());

        seat1A = seatRepository.save(Seat.builder()
                .bus(bus1)
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(1)
                .seatType(SeatType.SLEEPER)
                .isActive(true)
                .build());

        seat1B = seatRepository.save(Seat.builder()
                .bus(bus1)
                .seatNumber("1B")
                .deck(Deck.UPPER)
                .rowNum(1)
                .colNum(2)
                .seatType(SeatType.SLEEPER)
                .isActive(true)
                .build());

        Route route1 = routeRepository.save(Route.builder()
                .operator(op1)
                .routeCode("RT-DEL-MAN")
                .origin("Delhi")
                .destination("Manali")
                .distanceKm(530.0)
                .estimatedDurationMinutes(720)
                .isActive(true)
                .build());

        schedule1 = scheduleRepository.save(Schedule.builder()
                .bus(bus1)
                .route(route1)
                .operator(op1)
                .scheduleDate(LocalDate.of(2026, 12, 15))
                .departureTime(LocalDateTime.of(2026, 12, 15, 18, 0, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 16, 6, 0, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build());

        inventoryRepository.save(ScheduleSeatInventory.builder()
                .schedule(schedule1)
                .seat(seat1A)
                .status(SeatInventoryStatus.AVAILABLE)
                .build());

        inventoryRepository.save(ScheduleSeatInventory.builder()
                .schedule(schedule1)
                .seat(seat1B)
                .status(SeatInventoryStatus.AVAILABLE)
                .build());

        fareRepository.save(Fare.builder()
                .schedule(schedule1)
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("1200.00"))
                .taxAmount(new BigDecimal("60.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1260.00"))
                .currency("INR")
                .build());
    }

    @Test
    void shouldConfigureBusPhysicalSeats() throws Exception {
        CreateSeatRequest req1 = CreateSeatRequest.builder()
                .seatNumber("2A")
                .deck(Deck.LOWER)
                .rowNum(2)
                .colNum(1)
                .seatType(SeatType.SLEEPER)
                .build();

        CreateSeatRequest req2 = CreateSeatRequest.builder()
                .seatNumber("2B")
                .deck(Deck.UPPER)
                .rowNum(2)
                .colNum(2)
                .seatType(SeatType.SLEEPER)
                .build();

        BulkSeatConfigRequest bulkReq = BulkSeatConfigRequest.builder()
                .seats(List.of(req1, req2))
                .build();

        mockMvc.perform(post("/api/v1/buses/" + bus1.getId() + "/seats")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/buses/" + bus1.getId() + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    void shouldGetScheduleSeatMapAndBlockSeat() throws Exception {
        // 1. Get 2D seat map
        mockMvc.perform(get("/api/v1/schedules/" + schedule1.getId() + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lowerDeckSeats.length()").value(1))
                .andExpect(jsonPath("$.data.upperDeckSeats.length()").value(1))
                .andExpect(jsonPath("$.data.lowerDeckSeats[0].seatNumber").value("1A"))
                .andExpect(jsonPath("$.data.lowerDeckSeats[0].totalPrice").value(1260.00))
                .andExpect(jsonPath("$.data.upperDeckSeats[0].seatNumber").value("1B"));

        // 2. Block seat 1A
        BlockSeatRequest blockReq = BlockSeatRequest.builder().blocked(true).reason("VIP Reserve").build();
        mockMvc.perform(patch("/api/v1/schedules/" + schedule1.getId() + "/seats/1A/block")
                        .header("Authorization", operatorToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.isAvailable").value(false));

        // 3. Verify seat map reflects blocked status
        mockMvc.perform(get("/api/v1/schedules/" + schedule1.getId() + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableSeatsCount").value(1))
                .andExpect(jsonPath("$.data.lowerDeckSeats[0].status").value("BLOCKED"));
    }
}
