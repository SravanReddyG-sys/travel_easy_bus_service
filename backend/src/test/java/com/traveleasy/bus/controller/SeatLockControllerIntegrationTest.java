package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.ConfirmSeatsRequest;
import com.traveleasy.bus.dto.request.LockSeatsRequest;
import com.traveleasy.bus.dto.request.ReleaseSeatsRequest;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeatLockControllerIntegrationTest {

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

        BusOperator op1 = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP-LOCK-INT")
                .companyName("Lock Express Co")
                .contactEmail("op@lockexpress.com")
                .contactPhone("+911111111111")
                .build());

        Bus bus1 = busRepository.save(Bus.builder()
                .operator(op1)
                .busNumber("BUS-LK-100")
                .registrationNumber("DL-01-LK-1000")
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
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(2)
                .seatType(SeatType.SLEEPER)
                .isActive(true)
                .build());

        Route route1 = routeRepository.save(Route.builder()
                .operator(op1)
                .routeCode("RT-DEL-JAI-LK")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .build());

        schedule1 = scheduleRepository.save(Schedule.builder()
                .bus(bus1)
                .route(route1)
                .operator(op1)
                .scheduleDate(LocalDate.of(2026, 12, 20))
                .departureTime(LocalDateTime.of(2026, 12, 20, 9, 0, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 20, 15, 0, 0))
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
                .baseAmount(new BigDecimal("800.00"))
                .taxAmount(new BigDecimal("40.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("840.00"))
                .currency("INR")
                .build());
    }

    @Test
    void shouldLockAndConfirmSeatsSuccessfully() throws Exception {
        // 1. Lock seat 1A
        LockSeatsRequest lockReq = LockSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .bookingSessionId("sess_user_001")
                .ttlMinutes(10)
                .build();

        String lockRespContent = mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/seats/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockedSeats[0]").value("1A"))
                .andExpect(jsonPath("$.data.totalAmount").value(840.00))
                .andReturn().getResponse().getContentAsString();

        String lockToken = objectMapper.readTree(lockRespContent).path("data").path("lockToken").asText();

        // 2. Second session attempts to lock 1A -> Must fail with 409 Conflict
        LockSeatsRequest secondLockReq = LockSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .bookingSessionId("sess_user_002")
                .ttlMinutes(10)
                .build();

        mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/seats/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondLockReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUS_SEAT_ALREADY_LOCKED"));

        // 3. Confirm seat 1A with correct lock token and booking reference
        ConfirmSeatsRequest confirmReq = ConfirmSeatsRequest.builder()
                .seatNumbers(List.of("1A"))
                .lockToken(lockToken)
                .bookingReference("BKG-CONF-12345")
                .build();

        mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/seats/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookedSeats[0]").value("1A"))
                .andExpect(jsonPath("$.data.bookingReference").value("BKG-CONF-12345"));
    }

    @Test
    void shouldLockAndReleaseSeatsSuccessfully() throws Exception {
        // 1. Lock seat 1B
        LockSeatsRequest lockReq = LockSeatsRequest.builder()
                .seatNumbers(List.of("1B"))
                .bookingSessionId("sess_user_999")
                .ttlMinutes(10)
                .build();

        String lockRespContent = mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/seats/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String lockToken = objectMapper.readTree(lockRespContent).path("data").path("lockToken").asText();

        // 2. Release seat 1B
        ReleaseSeatsRequest releaseReq = ReleaseSeatsRequest.builder()
                .seatNumbers(List.of("1B"))
                .lockToken(lockToken)
                .build();

        mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/seats/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(releaseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.releasedSeats[0]").value("1B"));

        // 3. Seat 1B is now AVAILABLE and can be locked again immediately
        mockMvc.perform(post("/api/v1/schedules/" + schedule1.getId() + "/seats/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk());
    }
}
