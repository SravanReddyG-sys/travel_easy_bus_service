package com.traveleasy.bus.integration.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
import com.traveleasy.bus.integration.booking.dto.BookingSeatCancelRequest;
import com.traveleasy.bus.integration.booking.dto.BookingSeatConfirmRequest;
import com.traveleasy.bus.integration.booking.dto.BookingSeatHoldRequest;
import com.traveleasy.bus.repository.*;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusBookingIntegrationControllerIntegrationTest {

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

    private Schedule schedule;

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

        // 1. Operator
        BusOperator operator = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP-INTG-01")
                .companyName("Integration Express")
                .contactEmail("integration@bus.com")
                .contactPhone("9887766554")
                .isActive(true)
                .build());

        // 2. Bus
        Bus bus = busRepository.save(Bus.builder()
                .operator(operator)
                .busNumber("BUS-INTG-100")
                .registrationNumber("DL-01-IT-5555")
                .busType(BusType.AC_SEATER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .build());

        Seat seat1 = seatRepository.save(Seat.builder().bus(bus).seatNumber("1A").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(1).colNum(1).build());
        Seat seat2 = seatRepository.save(Seat.builder().bus(bus).seatNumber("1B").deck(Deck.LOWER).seatType(SeatType.SEATER).rowNum(1).colNum(2).build());

        // 3. Route
        Route route = routeRepository.save(Route.builder()
                .operator(operator)
                .routeCode("RT-INTG-01")
                .origin("Delhi")
                .destination("Agra")
                .distanceKm(200.0)
                .estimatedDurationMinutes(210)
                .isActive(true)
                .build());

        // 4. Schedule
        schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.now().plusDays(2))
                .departureTime(LocalDate.now().plusDays(2).atTime(8, 0))
                .arrivalTime(LocalDate.now().plusDays(2).atTime(11, 30))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .seatInventory(new ArrayList<>())
                .fares(new ArrayList<>())
                .build();

        ScheduleSeatInventory inv1 = ScheduleSeatInventory.builder().schedule(schedule).seat(seat1).status(SeatInventoryStatus.AVAILABLE).build();
        ScheduleSeatInventory inv2 = ScheduleSeatInventory.builder().schedule(schedule).seat(seat2).status(SeatInventoryStatus.AVAILABLE).build();
        schedule.getSeatInventory().add(inv1);
        schedule.getSeatInventory().add(inv2);

        Fare fare = Fare.builder()
                .schedule(schedule)
                .seatType(SeatType.SEATER)
                .baseAmount(BigDecimal.valueOf(400))
                .taxAmount(BigDecimal.valueOf(20))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(420))
                .currency("INR")
                .build();
        schedule.getFares().add(fare);

        schedule = scheduleRepository.save(schedule);
    }

    @Test
    @DisplayName("End-to-End Booking Integration Lifecycle: Hold -> Confirm -> Status -> Cancel")
    void testBookingIntegrationLifecycle() throws Exception {
        // Step 1: Hold Seats
        BookingSeatHoldRequest holdRequest = BookingSeatHoldRequest.builder()
                .scheduleId(schedule.getId())
                .seatNumbers(List.of("1A"))
                .holdDurationMinutes(10)
                .build();

        String holdResponseStr = mockMvc.perform(post("/api/v1/integration/booking/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lockedSeats[0]").value("1A"))
                .andExpect(jsonPath("$.data.totalHoldAmount").value(420.0))
                .andReturn().getResponse().getContentAsString();

        String lockToken = objectMapper.readTree(holdResponseStr).path("data").path("lockToken").asText();

        // Step 2: Confirm Booking
        BookingSeatConfirmRequest confirmRequest = BookingSeatConfirmRequest.builder()
                .scheduleId(schedule.getId())
                .bookingReference("BKG-INTG-2026-001")
                .lockToken(lockToken)
                .seatNumbers(List.of("1A"))
                .build();

        mockMvc.perform(post("/api/v1/integration/booking/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("BKG-INTG-2026-001"))
                .andExpect(jsonPath("$.data.confirmedSeats[0]").value("1A"))
                .andExpect(jsonPath("$.data.totalAmount").value(420.0));

        // Step 3: Query Booking Status
        mockMvc.perform(get("/api/v1/integration/booking/status/BKG-INTG-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("BKG-INTG-2026-001"))
                .andExpect(jsonPath("$.data.status").value("BOOKED"))
                .andExpect(jsonPath("$.data.seatNumbers[0]").value("1A"));

        // Step 4: Cancel Booking
        BookingSeatCancelRequest cancelRequest = BookingSeatCancelRequest.builder()
                .scheduleId(schedule.getId())
                .bookingReference("BKG-INTG-2026-001")
                .cancellationReason("Customer changed travel plan")
                .build();

        mockMvc.perform(post("/api/v1/integration/booking/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cancelled").value(true))
                .andExpect(jsonPath("$.data.releasedSeats[0]").value("1A"));
    }
}
