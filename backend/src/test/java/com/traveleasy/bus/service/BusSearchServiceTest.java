package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.BusSearchRequest;
import com.traveleasy.bus.dto.response.BusSearchPageResponse;
import com.traveleasy.bus.dto.response.BusSearchResultCardResponse;
import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.mapper.RouteMapper;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.service.impl.BusSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusSearchServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Mock
    private FareRepository fareRepository;

    @Spy
    private RouteMapper routeMapper = new RouteMapper();

    @InjectMocks
    private BusSearchServiceImpl busSearchService;

    private BusOperator operator;
    private Bus bus1;
    private Bus bus2;
    private Route route;
    private Schedule schedule1;
    private Schedule schedule2;
    private LocalDate travelDate;

    @BeforeEach
    void setUp() {
        travelDate = LocalDate.now().plusDays(5);

        operator = BusOperator.builder()
                .operatorCode("EXP-01")
                .companyName("Express Lines")
                .contactEmail("contact@express.com")
                .contactPhone("9876543210")
                .isActive(true)
                .build();
        operator.setId(UUID.randomUUID());

        bus1 = Bus.builder()
                .operator(operator)
                .busNumber("EXP-101")
                .registrationNumber("DL-01-AA-1111")
                .busType(BusType.AC_SEATER)
                .totalSeats(40)
                .status(BusStatus.ACTIVE)
                .amenities(List.of("WiFi", "Water Bottle"))
                .build();
        bus1.setId(UUID.randomUUID());

        bus2 = Bus.builder()
                .operator(operator)
                .busNumber("EXP-102")
                .registrationNumber("DL-01-BB-2222")
                .busType(BusType.VOLVO_MULTI_AXLE)
                .totalSeats(30)
                .status(BusStatus.ACTIVE)
                .amenities(List.of("WiFi", "Blanket", "Charging Point"))
                .build();
        bus2.setId(UUID.randomUUID());

        route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .boardingPoints(new ArrayList<>())
                .droppingPoints(new ArrayList<>())
                .build();
        route.setId(UUID.randomUUID());

        RouteStop originStop = RouteStop.builder()
                .route(route)
                .stopName("Delhi ISBT")
                .stopOrder(1)
                .distanceFromOriginKm(0.0)
                .build();
        originStop.setId(UUID.randomUUID());

        BoardingPoint bp = BoardingPoint.builder()
                .route(route)
                .stop(originStop)
                .locationName("Kashmere Gate Gate 1")
                .landmark("Near Metro Station")
                .departureOffsetMinutes(0)
                .contactPhone("9876543210")
                .build();
        bp.setId(UUID.randomUUID());
        route.getBoardingPoints().add(bp);

        RouteStop destStop = RouteStop.builder()
                .route(route)
                .stopName("Jaipur Sindhi Camp")
                .stopOrder(2)
                .distanceFromOriginKm(280.0)
                .build();
        destStop.setId(UUID.randomUUID());

        DroppingPoint dp = DroppingPoint.builder()
                .route(route)
                .stop(destStop)
                .locationName("Sindhi Camp Bus Stand")
                .landmark("Platform 3")
                .arrivalOffsetMinutes(300)
                .contactPhone("9876543211")
                .build();
        dp.setId(UUID.randomUUID());
        route.getDroppingPoints().add(dp);

        schedule1 = Schedule.builder()
                .bus(bus1)
                .route(route)
                .operator(operator)
                .scheduleDate(travelDate)
                .departureTime(travelDate.atTime(8, 0))
                .arrivalTime(travelDate.atTime(13, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .fares(new ArrayList<>())
                .build();
        schedule1.setId(UUID.randomUUID());

        Fare fare1 = Fare.builder()
                .schedule(schedule1)
                .seatType(SeatType.SEATER)
                .baseAmount(BigDecimal.valueOf(500))
                .taxAmount(BigDecimal.valueOf(25))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(525))
                .currency("INR")
                .build();
        fare1.setId(UUID.randomUUID());
        schedule1.getFares().add(fare1);

        schedule2 = Schedule.builder()
                .bus(bus2)
                .route(route)
                .operator(operator)
                .scheduleDate(travelDate)
                .departureTime(travelDate.atTime(14, 0))
                .arrivalTime(travelDate.atTime(19, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .fares(new ArrayList<>())
                .build();
        schedule2.setId(UUID.randomUUID());

        Fare fare2 = Fare.builder()
                .schedule(schedule2)
                .seatType(SeatType.SLEEPER)
                .baseAmount(BigDecimal.valueOf(900))
                .taxAmount(BigDecimal.valueOf(45))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(945))
                .currency("INR")
                .build();
        fare2.setId(UUID.randomUUID());
        schedule2.getFares().add(fare2);
    }

    @Test
    @DisplayName("Should successfully search buses matching origin, destination and date")
    void searchBuses_Success() {
        when(scheduleRepository.findAvailableSchedulesForSearch(eq("Delhi"), eq("Jaipur"), eq(travelDate)))
                .thenReturn(List.of(schedule1, schedule2));
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule1.getId())).thenReturn(25L);
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule2.getId())).thenReturn(10L);

        BusSearchRequest request = BusSearchRequest.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .travelDate(travelDate)
                .build();

        BusSearchPageResponse response = busSearchService.searchBuses(request);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).hasSize(2);

        BusSearchResultCardResponse card1 = response.getContent().get(0);
        assertThat(card1.getBusNumber()).isEqualTo("EXP-101");
        assertThat(card1.getStartingFare()).isEqualByComparingTo("525.00");
        assertThat(card1.getAvailableSeatsCount()).isEqualTo(25L);
        assertThat(card1.getBoardingPoints()).hasSize(1);
        assertThat(card1.getDroppingPoints()).hasSize(1);

        assertThat(response.getFilterSummary()).isNotNull();
        assertThat(response.getFilterSummary().getAvailableBusTypes()).contains(BusType.AC_SEATER, BusType.VOLVO_MULTI_AXLE);
        assertThat(response.getFilterSummary().getMinPrice()).isEqualByComparingTo("525.00");
        assertThat(response.getFilterSummary().getMaxPrice()).isEqualByComparingTo("945.00");
    }

    @Test
    @DisplayName("Should filter search results by BusType")
    void searchBuses_FilterByBusType() {
        when(scheduleRepository.findAvailableSchedulesForSearch(eq("Delhi"), eq("Jaipur"), eq(travelDate)))
                .thenReturn(List.of(schedule1, schedule2));
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule1.getId())).thenReturn(25L);
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule2.getId())).thenReturn(10L);

        BusSearchRequest request = BusSearchRequest.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .travelDate(travelDate)
                .busType(BusType.VOLVO_MULTI_AXLE)
                .build();

        BusSearchPageResponse response = busSearchService.searchBuses(request);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getBusNumber()).isEqualTo("EXP-102");
    }

    @Test
    @DisplayName("Should filter search results by price range")
    void searchBuses_FilterByPriceRange() {
        when(scheduleRepository.findAvailableSchedulesForSearch(eq("Delhi"), eq("Jaipur"), eq(travelDate)))
                .thenReturn(List.of(schedule1, schedule2));
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule1.getId())).thenReturn(25L);
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule2.getId())).thenReturn(10L);

        BusSearchRequest request = BusSearchRequest.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .travelDate(travelDate)
                .minPrice(BigDecimal.valueOf(600))
                .maxPrice(BigDecimal.valueOf(1000))
                .build();

        BusSearchPageResponse response = busSearchService.searchBuses(request);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getBusNumber()).isEqualTo("EXP-102");
        assertThat(response.getContent().get(0).getStartingFare()).isEqualByComparingTo("945.00");
    }

    @Test
    @DisplayName("Should sort results by price descending")
    void searchBuses_SortByPriceDesc() {
        when(scheduleRepository.findAvailableSchedulesForSearch(eq("Delhi"), eq("Jaipur"), eq(travelDate)))
                .thenReturn(List.of(schedule1, schedule2));
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule1.getId())).thenReturn(25L);
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule2.getId())).thenReturn(10L);

        BusSearchRequest request = BusSearchRequest.builder()
                .origin("Delhi")
                .destination("Jaipur")
                .travelDate(travelDate)
                .sortBy("price_desc")
                .build();

        BusSearchPageResponse response = busSearchService.searchBuses(request);

        assertThat(response.getContent().get(0).getBusNumber()).isEqualTo("EXP-102");
        assertThat(response.getContent().get(1).getBusNumber()).isEqualTo("EXP-101");
    }

    @Test
    @DisplayName("Should return empty result when no schedules exist")
    void searchBuses_EmptyResult() {
        when(scheduleRepository.findAvailableSchedulesForSearch(eq("Delhi"), eq("Agra"), eq(travelDate)))
                .thenReturn(List.of());

        BusSearchRequest request = BusSearchRequest.builder()
                .origin("Delhi")
                .destination("Agra")
                .travelDate(travelDate)
                .build();

        BusSearchPageResponse response = busSearchService.searchBuses(request);

        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getContent()).isEmpty();
    }
}
