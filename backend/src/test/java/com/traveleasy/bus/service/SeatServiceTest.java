package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.BlockSeatRequest;
import com.traveleasy.bus.dto.request.BulkSeatConfigRequest;
import com.traveleasy.bus.dto.request.CreateSeatRequest;
import com.traveleasy.bus.dto.response.PhysicalSeatResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatInventoryResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatMapResponse;
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
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.DuplicateResourceException;
import com.traveleasy.bus.mapper.SeatMapper;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.repository.SeatRepository;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.impl.SeatServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private FareRepository fareRepository;

    @Spy
    private SeatMapper seatMapper = new SeatMapper();

    @InjectMocks
    private SeatServiceImpl seatService;

    private UUID operatorId;
    private Bus bus;
    private Schedule schedule;
    private Seat seat1;
    private ScheduleSeatInventory inventory1;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        BusOperator operator = BusOperator.builder()
                .operatorCode("OP-SEAT-01")
                .companyName("Seat Master Co")
                .build();
        operator.setId(operatorId);

        bus = Bus.builder()
                .operator(operator)
                .busNumber("BUS-ST-1")
                .registrationNumber("DL-01-ST-1111")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(1)
                .status(BusStatus.ACTIVE)
                .build();
        bus.setId(UUID.randomUUID());

        Route route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .build();
        route.setId(UUID.randomUUID());

        schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.of(2026, 12, 1))
                .departureTime(LocalDateTime.of(2026, 12, 1, 8, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 14, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();
        schedule.setId(UUID.randomUUID());

        seat1 = Seat.builder()
                .bus(bus)
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(1)
                .seatType(SeatType.SLEEPER)
                .isActive(true)
                .build();
        seat1.setId(UUID.randomUUID());

        inventory1 = ScheduleSeatInventory.builder()
                .schedule(schedule)
                .seat(seat1)
                .status(SeatInventoryStatus.AVAILABLE)
                .build();
        inventory1.setId(UUID.randomUUID());

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("op@seatmaster.com")
                .operatorId(operatorId)
                .roles(Set.of(UserRole.ROLE_OPERATOR))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldConfigureBusSeatsSuccessfully() {
        CreateSeatRequest req1 = CreateSeatRequest.builder()
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(1)
                .seatType(SeatType.SLEEPER)
                .build();

        CreateSeatRequest req2 = CreateSeatRequest.builder()
                .seatNumber("1B")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(2)
                .seatType(SeatType.SLEEPER)
                .build();

        BulkSeatConfigRequest bulkReq = BulkSeatConfigRequest.builder()
                .seats(List.of(req1, req2))
                .build();

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));
        when(seatRepository.findByBusIdAndSeatNumber(bus.getId(), "1A")).thenReturn(Optional.empty());
        when(seatRepository.findByBusIdAndSeatNumber(bus.getId(), "1B")).thenReturn(Optional.empty());
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));
        when(seatRepository.countByBusId(bus.getId())).thenReturn(2L);

        List<PhysicalSeatResponse> responses = seatService.configureBusSeats(bus.getId(), bulkReq);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("1A", responses.get(0).getSeatNumber());
        assertEquals("1B", responses.get(1).getSeatNumber());
        verify(busRepository).save(bus);
    }

    @Test
    void shouldRejectBulkSeatConfigWithDuplicateSeatNumbers() {
        CreateSeatRequest req1 = CreateSeatRequest.builder()
                .seatNumber("1A")
                .deck(Deck.LOWER)
                .rowNum(1)
                .colNum(1)
                .seatType(SeatType.SLEEPER)
                .build();

        CreateSeatRequest req2 = CreateSeatRequest.builder()
                .seatNumber("1A") // Duplicate
                .deck(Deck.UPPER)
                .rowNum(1)
                .colNum(1)
                .seatType(SeatType.SLEEPER)
                .build();

        BulkSeatConfigRequest bulkReq = BulkSeatConfigRequest.builder()
                .seats(List.of(req1, req2))
                .build();

        when(busRepository.findById(bus.getId())).thenReturn(Optional.of(bus));

        assertThrows(DuplicateResourceException.class, () -> seatService.configureBusSeats(bus.getId(), bulkReq));
        verify(seatRepository, never()).save(any());
    }

    @Test
    void shouldGetScheduleSeatMapWithLiveFares() {
        Fare fare = Fare.builder()
                .schedule(schedule)
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("750.00"))
                .taxAmount(new BigDecimal("37.50"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("787.50"))
                .currency("INR")
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdWithSeats(schedule.getId())).thenReturn(List.of(inventory1));
        when(fareRepository.findByScheduleId(schedule.getId())).thenReturn(List.of(fare));

        ScheduleSeatMapResponse seatMap = seatService.getScheduleSeatMap(schedule.getId());

        assertNotNull(seatMap);
        assertEquals(1, seatMap.getLowerDeckSeats().size());
        ScheduleSeatInventoryResponse seatResp = seatMap.getLowerDeckSeats().get(0);
        assertEquals("1A", seatResp.getSeatNumber());
        assertTrue(seatResp.getIsAvailable());
        assertEquals(new BigDecimal("750.00"), seatResp.getBasePrice());
        assertEquals(new BigDecimal("787.50"), seatResp.getTotalPrice());
    }

    @Test
    void shouldBlockAndUnblockSeatSuccessfully() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumber(schedule.getId(), "1A")).thenReturn(Optional.of(inventory1));
        when(inventoryRepository.save(any(ScheduleSeatInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        // Block
        BlockSeatRequest blockReq = BlockSeatRequest.builder().blocked(true).reason("Crew seat").build();
        ScheduleSeatInventoryResponse blockedResp = seatService.blockOrUnblockSeat(schedule.getId(), "1A", blockReq);

        assertNotNull(blockedResp);
        assertEquals(SeatInventoryStatus.BLOCKED, blockedResp.getStatus());
        assertFalse(blockedResp.getIsAvailable());

        // Unblock
        BlockSeatRequest unblockReq = BlockSeatRequest.builder().blocked(false).build();
        ScheduleSeatInventoryResponse unblockedResp = seatService.blockOrUnblockSeat(schedule.getId(), "1A", unblockReq);

        assertNotNull(unblockedResp);
        assertEquals(SeatInventoryStatus.AVAILABLE, unblockedResp.getStatus());
        assertTrue(unblockedResp.getIsAvailable());
    }

    @Test
    void shouldRejectBlockingBookedSeat() {
        inventory1.setStatus(SeatInventoryStatus.BOOKED);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(inventoryRepository.findByScheduleIdAndSeatNumber(schedule.getId(), "1A")).thenReturn(Optional.of(inventory1));

        BlockSeatRequest blockReq = BlockSeatRequest.builder().blocked(true).build();

        assertThrows(BusDomainException.class, () -> seatService.blockOrUnblockSeat(schedule.getId(), "1A", blockReq));
    }
}
