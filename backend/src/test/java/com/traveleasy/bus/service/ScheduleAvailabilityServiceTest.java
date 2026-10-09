package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.UpdateScheduleAvailabilityRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleStatusRequest;
import com.traveleasy.bus.dto.response.ScheduleAvailabilityHistoryResponse;
import com.traveleasy.bus.dto.response.ScheduleResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusAvailability;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.mapper.ScheduleMapper;
import com.traveleasy.bus.repository.BusAvailabilityRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.impl.ScheduleAvailabilityServiceImpl;
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
class ScheduleAvailabilityServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleSeatInventoryRepository inventoryRepository;

    @Mock
    private BusAvailabilityRepository busAvailabilityRepository;

    @Spy
    private ScheduleMapper scheduleMapper = new ScheduleMapper();

    @InjectMocks
    private ScheduleAvailabilityServiceImpl availabilityService;

    private UUID operatorId;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        BusOperator operator = BusOperator.builder()
                .operatorCode("OP-AVAIL-01")
                .companyName("Avail Express")
                .build();
        operator.setId(operatorId);

        Bus bus = Bus.builder()
                .operator(operator)
                .busNumber("BUS-AV1")
                .registrationNumber("DL-01-AV-9999")
                .busType(BusType.AC_SEATER)
                .totalSeats(40)
                .status(BusStatus.ACTIVE)
                .build();
        bus.setId(UUID.randomUUID());

        Route route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-AGR")
                .origin("Delhi")
                .destination("Agra")
                .distanceKm(220.0)
                .estimatedDurationMinutes(240)
                .isActive(true)
                .build();
        route.setId(UUID.randomUUID());

        schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(LocalDate.of(2026, 12, 1))
                .departureTime(LocalDateTime.of(2026, 12, 1, 6, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 10, 0))
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();
        schedule.setId(UUID.randomUUID());

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("op@availexpress.com")
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
    void shouldUpdateScheduleAvailabilitySuccessfully() {
        UpdateScheduleAvailabilityRequest request = UpdateScheduleAvailabilityRequest.builder()
                .availabilityStatus(AvailabilityStatus.UNAVAILABLE)
                .reason("Severe weather warning")
                .build();

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.countAvailableSeatsByScheduleId(schedule.getId())).thenReturn(30L);

        ScheduleResponse response = availabilityService.updateScheduleAvailability(schedule.getId(), request);

        assertNotNull(response);
        assertEquals(AvailabilityStatus.UNAVAILABLE, schedule.getAvailabilityStatus());
        verify(busAvailabilityRepository).save(any(BusAvailability.class));
    }

    @Test
    void shouldFollowValidStateTransitions() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. SCHEDULED -> BOARDING
        UpdateScheduleStatusRequest boardingReq = UpdateScheduleStatusRequest.builder()
                .status(ScheduleStatus.BOARDING)
                .build();
        availabilityService.updateScheduleStatus(schedule.getId(), boardingReq);
        assertEquals(ScheduleStatus.BOARDING, schedule.getStatus());

        // 2. BOARDING -> DEPARTED
        UpdateScheduleStatusRequest departedReq = UpdateScheduleStatusRequest.builder()
                .status(ScheduleStatus.DEPARTED)
                .build();
        availabilityService.updateScheduleStatus(schedule.getId(), departedReq);
        assertEquals(ScheduleStatus.DEPARTED, schedule.getStatus());

        // 3. DEPARTED -> COMPLETED
        UpdateScheduleStatusRequest completedReq = UpdateScheduleStatusRequest.builder()
                .status(ScheduleStatus.COMPLETED)
                .build();
        availabilityService.updateScheduleStatus(schedule.getId(), completedReq);
        assertEquals(ScheduleStatus.COMPLETED, schedule.getStatus());
        assertEquals(AvailabilityStatus.UNAVAILABLE, schedule.getAvailabilityStatus());
    }

    @Test
    void shouldRejectInvalidStateTransition() {
        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        // SCHEDULED -> COMPLETED directly (invalid)
        UpdateScheduleStatusRequest invalidReq = UpdateScheduleStatusRequest.builder()
                .status(ScheduleStatus.COMPLETED)
                .build();

        assertThrows(BusDomainException.class, () -> availabilityService.updateScheduleStatus(schedule.getId(), invalidReq));
    }

    @Test
    void shouldGetAvailabilityAuditHistory() {
        BusAvailability audit = BusAvailability.builder()
                .schedule(schedule)
                .status(AvailabilityStatus.UNAVAILABLE)
                .reason("Engine check")
                .modifiedBy("op@availexpress.com")
                .effectiveFrom(LocalDateTime.now())
                .build();
        audit.setId(UUID.randomUUID());

        when(scheduleRepository.existsById(schedule.getId())).thenReturn(true);
        when(busAvailabilityRepository.findByScheduleIdOrderByEffectiveFromDesc(schedule.getId()))
                .thenReturn(List.of(audit));

        List<ScheduleAvailabilityHistoryResponse> history = availabilityService.getScheduleAvailabilityHistory(schedule.getId());

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(AvailabilityStatus.UNAVAILABLE, history.get(0).getStatus());
        assertEquals("Engine check", history.get(0).getReason());
    }
}
