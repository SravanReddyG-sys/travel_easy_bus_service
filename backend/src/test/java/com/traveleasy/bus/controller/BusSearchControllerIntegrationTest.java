package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusSearchControllerIntegrationTest {

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

    private LocalDate travelDate;

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

        travelDate = LocalDate.now().plusDays(3);

        // 1. Create Operator
        BusOperator operator = operatorRepository.save(BusOperator.builder()
                .operatorCode("OP-SEARCH-01")
                .companyName("Search Travel Express")
                .contactEmail("search@express.com")
                .contactPhone("9998887776")
                .isActive(true)
                .build());

        // 2. Create Buses
        Bus bus1 = busRepository.save(Bus.builder()
                .operator(operator)
                .busNumber("BUS-SEARCH-101")
                .registrationNumber("DL-01-SR-1111")
                .busType(BusType.AC_SEATER)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .amenities(List.of("WiFi", "Charging Point"))
                .build());

        Seat bus1Seat1 = seatRepository.save(Seat.builder()
                .bus(bus1)
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .seatType(SeatType.SEATER)
                .rowNum(1)
                .colNum(1)
                .isActive(true)
                .build());

        Seat bus1Seat2 = seatRepository.save(Seat.builder()
                .bus(bus1)
                .seatNumber("1B")
                .deck(Deck.LOWER)
                .seatType(SeatType.SEATER)
                .rowNum(1)
                .colNum(2)
                .isActive(true)
                .build());

        Bus bus2 = busRepository.save(Bus.builder()
                .operator(operator)
                .busNumber("BUS-SEARCH-102")
                .registrationNumber("DL-01-SR-2222")
                .busType(BusType.VOLVO_MULTI_AXLE)
                .totalSeats(2)
                .status(BusStatus.ACTIVE)
                .amenities(List.of("WiFi", "Blanket", "Water Bottle"))
                .build());

        Seat bus2Seat1 = seatRepository.save(Seat.builder()
                .bus(bus2)
                .seatNumber("2A")
                .deck(Deck.LOWER)
                .seatType(SeatType.SLEEPER)
                .rowNum(1)
                .colNum(1)
                .isActive(true)
                .build());

        Seat bus2Seat2 = seatRepository.save(Seat.builder()
                .bus(bus2)
                .seatNumber("2B")
                .deck(Deck.LOWER)
                .seatType(SeatType.SLEEPER)
                .rowNum(1)
                .colNum(2)
                .isActive(true)
                .build());

        // 3. Create Route with Boarding & Dropping Points
        Route route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI-SR")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(260.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .stops(new ArrayList<>())
                .boardingPoints(new ArrayList<>())
                .droppingPoints(new ArrayList<>())
                .build();

        RouteStop stop1 = RouteStop.builder()
                .route(route)
                .stopName("Kashmere Gate")
                .stopOrder(1)
                .distanceFromOriginKm(0.0)
                .build();
        route.getStops().add(stop1);

        BoardingPoint bp = BoardingPoint.builder()
                .route(route)
                .stop(stop1)
                .locationName("Kashmere Gate Metro Gate 2")
                .landmark("Near Metro Exit")
                .departureOffsetMinutes(0)
                .contactPhone("9998887776")
                .build();
        route.getBoardingPoints().add(bp);

        RouteStop stop2 = RouteStop.builder()
                .route(route)
                .stopName("Sindhi Camp")
                .stopOrder(2)
                .distanceFromOriginKm(260.0)
                .build();
        route.getStops().add(stop2);

        DroppingPoint dp = DroppingPoint.builder()
                .route(route)
                .stop(stop2)
                .locationName("Sindhi Camp Bus Station")
                .landmark("Platform 1")
                .arrivalOffsetMinutes(300)
                .contactPhone("9998887776")
                .build();
        route.getDroppingPoints().add(dp);

        route = routeRepository.save(route);

        // 4. Create Schedules
        Schedule schedule1 = Schedule.builder()
                .bus(bus1)
                .route(route)
                .operator(operator)
                .scheduleDate(travelDate)
                .departureTime(travelDate.atTime(6, 0))
                .arrivalTime(travelDate.atTime(11, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .seatInventory(new ArrayList<>())
                .fares(new ArrayList<>())
                .build();

        ScheduleSeatInventory inv1A = ScheduleSeatInventory.builder()
                .schedule(schedule1)
                .seat(bus1Seat1)
                .status(SeatInventoryStatus.AVAILABLE)
                .build();
        ScheduleSeatInventory inv1B = ScheduleSeatInventory.builder()
                .schedule(schedule1)
                .seat(bus1Seat2)
                .status(SeatInventoryStatus.AVAILABLE)
                .build();
        schedule1.getSeatInventory().add(inv1A);
        schedule1.getSeatInventory().add(inv1B);

        Fare fare1 = Fare.builder()
                .schedule(schedule1)
                .seatType(SeatType.SEATER)
                .baseAmount(BigDecimal.valueOf(450.00))
                .taxAmount(BigDecimal.valueOf(25.00))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(475.00))
                .currency("INR")
                .build();
        schedule1.getFares().add(fare1);

        scheduleRepository.save(schedule1);

        Schedule schedule2 = Schedule.builder()
                .bus(bus2)
                .route(route)
                .operator(operator)
                .scheduleDate(travelDate)
                .departureTime(travelDate.atTime(15, 0))
                .arrivalTime(travelDate.atTime(20, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .seatInventory(new ArrayList<>())
                .fares(new ArrayList<>())
                .build();

        ScheduleSeatInventory inv2A = ScheduleSeatInventory.builder()
                .schedule(schedule2)
                .seat(bus2Seat1)
                .status(SeatInventoryStatus.AVAILABLE)
                .build();
        ScheduleSeatInventory inv2B = ScheduleSeatInventory.builder()
                .schedule(schedule2)
                .seat(bus2Seat2)
                .status(SeatInventoryStatus.BOOKED) // One seat booked
                .build();
        schedule2.getSeatInventory().add(inv2A);
        schedule2.getSeatInventory().add(inv2B);

        Fare fare2 = Fare.builder()
                .schedule(schedule2)
                .seatType(SeatType.SLEEPER)
                .baseAmount(BigDecimal.valueOf(800.00))
                .taxAmount(BigDecimal.valueOf(50.00))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(850.00))
                .currency("INR")
                .build();
        schedule2.getFares().add(fare2);

        scheduleRepository.save(schedule2);
    }

    @Test
    @DisplayName("Public Search: GET /api/v1/buses/search returns matching bus schedules")
    void searchBuses_PublicEndpoint_Success() throws Exception {
        mockMvc.perform(get("/api/v1/buses/search")
                        .param("origin", "Delhi")
                        .param("destination", "Jaipur")
                        .param("travelDate", travelDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].origin").value("Delhi"))
                .andExpect(jsonPath("$.data.content[0].destination").value("Jaipur"))
                .andExpect(jsonPath("$.data.content[0].startingFare").value(475.0))
                .andExpect(jsonPath("$.data.content[0].availableSeatsCount").value(2))
                .andExpect(jsonPath("$.data.content[1].startingFare").value(850.0))
                .andExpect(jsonPath("$.data.content[1].availableSeatsCount").value(1))
                .andExpect(jsonPath("$.data.filterSummary.totalFound").value(2))
                .andExpect(jsonPath("$.data.filterSummary.minPrice").value(475.0))
                .andExpect(jsonPath("$.data.filterSummary.maxPrice").value(850.0));
    }

    @Test
    @DisplayName("Public Search: Filter by busType and price range")
    void searchBuses_FilterByBusTypeAndPrice() throws Exception {
        mockMvc.perform(get("/api/v1/buses/search")
                        .param("origin", "Delhi")
                        .param("destination", "Jaipur")
                        .param("travelDate", travelDate.toString())
                        .param("busType", "VOLVO_MULTI_AXLE")
                        .param("minPrice", "500")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].busNumber").value("BUS-SEARCH-102"))
                .andExpect(jsonPath("$.data.content[0].busType").value("VOLVO_MULTI_AXLE"))
                .andExpect(jsonPath("$.data.content[0].startingFare").value(850.0));
    }

    @Test
    @DisplayName("Public Search: Sort by price descending")
    void searchBuses_SortByPriceDesc() throws Exception {
        mockMvc.perform(get("/api/v1/buses/search")
                        .param("origin", "Delhi")
                        .param("destination", "Jaipur")
                        .param("travelDate", travelDate.toString())
                        .param("sortBy", "price_desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].busNumber").value("BUS-SEARCH-102"))
                .andExpect(jsonPath("$.data.content[1].busNumber").value("BUS-SEARCH-101"));
    }

    @Test
    @DisplayName("Public Search: Missing required params should return 400 Bad Request")
    void searchBuses_MissingParams_BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/buses/search")
                        .param("origin", "Delhi")
                        // missing destination and travelDate
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
