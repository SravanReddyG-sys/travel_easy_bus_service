package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.CreateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusRequest;
import com.traveleasy.bus.dto.response.BusResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.exception.DuplicateResourceException;
import com.traveleasy.bus.exception.ForbiddenAccessException;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.BusMapper;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.impl.BusServiceImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusServiceTest {

    @Mock
    private BusRepository busRepository;

    @Mock
    private BusOperatorRepository operatorRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Spy
    private BusMapper busMapper = new BusMapper();

    @InjectMocks
    private BusServiceImpl busService;

    private UUID operatorId;
    private BusOperator operator;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        operator = BusOperator.builder()
                .operatorCode("OP-TEST")
                .companyName("Test Bus Lines")
                .contactEmail("test@bus.com")
                .contactPhone("+919876543210")
                .build();
        operator.setId(operatorId);

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("test@bus.com")
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
    void shouldCreateBusSuccessfully() {
        CreateBusRequest request = CreateBusRequest.builder()
                .busNumber("BUS-101")
                .registrationNumber("DL-01-AB-1234")
                .busType(BusType.AC_SEATER)
                .totalSeats(40)
                .amenities(List.of("WiFi", "AC"))
                .build();

        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(busRepository.existsByRegistrationNumber("DL-01-AB-1234")).thenReturn(false);
        when(busRepository.existsByOperatorIdAndBusNumber(operatorId, "BUS-101")).thenReturn(false);

        Bus savedBus = Bus.builder()
                .operator(operator)
                .busNumber("BUS-101")
                .registrationNumber("DL-01-AB-1234")
                .busType(BusType.AC_SEATER)
                .totalSeats(40)
                .status(BusStatus.ACTIVE)
                .amenities(List.of("WiFi", "AC"))
                .build();
        savedBus.setId(UUID.randomUUID());

        when(busRepository.save(any(Bus.class))).thenReturn(savedBus);

        BusResponse response = busService.createBus(request);

        assertNotNull(response);
        assertEquals("BUS-101", response.getBusNumber());
        assertEquals("DL-01-AB-1234", response.getRegistrationNumber());
        assertEquals(40, response.getTotalSeats());
        assertEquals(operatorId, response.getOperatorId());
        verify(busRepository).save(any(Bus.class));
    }

    @Test
    void shouldRejectDuplicateRegistrationNumber() {
        CreateBusRequest request = CreateBusRequest.builder()
                .busNumber("BUS-102")
                .registrationNumber("DL-01-AB-1234")
                .busType(BusType.AC_SEATER)
                .totalSeats(40)
                .build();

        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(busRepository.existsByRegistrationNumber("DL-01-AB-1234")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> busService.createBus(request));
        verify(busRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundForNonExistingBus() {
        UUID nonExistingId = UUID.randomUUID();
        when(busRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> busService.getBusById(nonExistingId));
    }

    @Test
    void shouldRejectModificationByDifferentOperator() {
        UUID differentOperatorId = UUID.randomUUID();
        BusOperator otherOperator = BusOperator.builder()
                .operatorCode("OTHER-OP")
                .companyName("Other Operator")
                .build();
        otherOperator.setId(differentOperatorId);

        Bus otherBus = Bus.builder()
                .operator(otherOperator)
                .busNumber("BUS-999")
                .registrationNumber("KA-01-XX-9999")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(30)
                .build();
        UUID otherBusId = UUID.randomUUID();
        otherBus.setId(otherBusId);

        when(busRepository.findById(otherBusId)).thenReturn(Optional.of(otherBus));

        UpdateBusRequest updateRequest = UpdateBusRequest.builder()
                .busNumber("BUS-999-MOD")
                .busType(BusType.AC_SLEEPER)
                .totalSeats(30)
                .status(BusStatus.ACTIVE)
                .build();

        // Should throw ForbiddenAccessException because authenticated operator != bus owner
        assertThrows(ForbiddenAccessException.class, () -> busService.updateBus(otherBusId, updateRequest));
        verify(busRepository, never()).save(any());
    }
}
