package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.CreateFareRequest;
import com.traveleasy.bus.dto.request.UpdateFareRequest;
import com.traveleasy.bus.dto.response.FareResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.ForbiddenAccessException;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.FareMapper;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.impl.FareServiceImpl;
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
class FareServiceTest {

    @Mock
    private FareRepository fareRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Spy
    private FareMapper fareMapper = new FareMapper();

    @InjectMocks
    private FareServiceImpl fareService;

    private UUID operatorId;
    private BusOperator operator;
    private Schedule schedule;
    private Fare fare;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        operator = BusOperator.builder()
                .operatorCode("OP-FARE-01")
                .companyName("Fare Express")
                .build();
        operator.setId(operatorId);

        Bus bus = Bus.builder()
                .operator(operator)
                .busNumber("BUS-F1")
                .registrationNumber("DL-01-FA-1000")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(30)
                .status(BusStatus.ACTIVE)
                .build();
        bus.setId(UUID.randomUUID());

        Route route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-CHD")
                .origin("Delhi")
                .destination("Chandigarh")
                .distanceKm(250.0)
                .estimatedDurationMinutes(300)
                .isActive(true)
                .build();
        route.setId(UUID.randomUUID());

        schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.of(2026, 12, 1))
                .departureTime(LocalDateTime.of(2026, 12, 1, 6, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 11, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();
        schedule.setId(UUID.randomUUID());

        fare = Fare.builder()
                .schedule(schedule)
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("800.00"))
                .taxAmount(new BigDecimal("40.00"))
                .discountAmount(new BigDecimal("0.00"))
                .totalAmount(new BigDecimal("840.00"))
                .currency("INR")
                .build();
        fare.setId(UUID.randomUUID());

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("op@fareexpress.com")
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
    void shouldSetFareForSeatTypeSuccessfully() {
        CreateFareRequest request = CreateFareRequest.builder()
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("800.00"))
                .taxAmount(new BigDecimal("40.00"))
                .discountAmount(BigDecimal.ZERO)
                .currency("INR")
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(fareRepository.findByScheduleIdAndSeatType(schedule.getId(), SeatType.SLEEPER)).thenReturn(Optional.empty());
        when(fareRepository.save(any(Fare.class))).thenReturn(fare);

        FareResponse response = fareService.setFareForSeatType(schedule.getId(), request);

        assertNotNull(response);
        assertEquals(SeatType.SLEEPER, response.getSeatType());
        assertEquals(new BigDecimal("800.00"), response.getBaseAmount());
        assertEquals(new BigDecimal("840.00"), response.getTotalAmount());
        assertEquals("INR", response.getCurrency());
        verify(fareRepository).save(any(Fare.class));
    }

    @Test
    void shouldUpdateExistingFareWhenAlreadyConfigured() {
        CreateFareRequest request = CreateFareRequest.builder()
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("900.00"))
                .taxAmount(new BigDecimal("45.00"))
                .discountAmount(new BigDecimal("50.00"))
                .currency("INR")
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(fareRepository.findByScheduleIdAndSeatType(schedule.getId(), SeatType.SLEEPER)).thenReturn(Optional.of(fare));
        when(fareRepository.save(any(Fare.class))).thenReturn(fare);

        FareResponse response = fareService.setFareForSeatType(schedule.getId(), request);

        assertNotNull(response);
        assertEquals(new BigDecimal("900.00"), fare.getBaseAmount());
        assertEquals(new BigDecimal("45.00"), fare.getTaxAmount());
        assertEquals(new BigDecimal("50.00"), fare.getDiscountAmount());
        assertEquals(new BigDecimal("895.00"), fare.getTotalAmount());
    }

    @Test
    void shouldRejectFareWhenDiscountExceedsTotalGrossAmount() {
        CreateFareRequest request = CreateFareRequest.builder()
                .seatType(SeatType.SLEEPER)
                .baseAmount(new BigDecimal("500.00"))
                .taxAmount(new BigDecimal("25.00"))
                .discountAmount(new BigDecimal("600.00")) // Exceeds base + tax (525.00)
                .currency("INR")
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        assertThrows(BusDomainException.class, () -> fareService.setFareForSeatType(schedule.getId(), request));
        verify(fareRepository, never()).save(any());
    }

    @Test
    void shouldRejectSettingFareForAnotherOperatorsSchedule() {
        UUID otherOperatorId = UUID.randomUUID();
        BusOperator otherOperator = BusOperator.builder()
                .operatorCode("OTHER")
                .build();
        otherOperator.setId(otherOperatorId);

        schedule.setOperator(otherOperator);

        CreateFareRequest request = CreateFareRequest.builder()
                .seatType(SeatType.SEATER)
                .baseAmount(new BigDecimal("400.00"))
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        assertThrows(ForbiddenAccessException.class, () -> fareService.setFareForSeatType(schedule.getId(), request));
        verify(fareRepository, never()).save(any());
    }

    @Test
    void shouldGetFaresByScheduleIdSuccessfully() {
        when(scheduleRepository.existsById(schedule.getId())).thenReturn(true);
        when(fareRepository.findByScheduleId(schedule.getId())).thenReturn(List.of(fare));

        List<FareResponse> responses = fareService.getFaresByScheduleId(schedule.getId());

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(SeatType.SLEEPER, responses.get(0).getSeatType());
    }
}
