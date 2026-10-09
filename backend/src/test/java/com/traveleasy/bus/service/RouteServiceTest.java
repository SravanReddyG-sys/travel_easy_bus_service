package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.BoardingPointRequest;
import com.traveleasy.bus.dto.request.CreateRouteRequest;
import com.traveleasy.bus.dto.request.RouteStopRequest;
import com.traveleasy.bus.dto.response.BoardingPointResponse;
import com.traveleasy.bus.dto.response.RouteResponse;
import com.traveleasy.bus.dto.response.RouteStopResponse;
import com.traveleasy.bus.entity.BoardingPoint;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.RouteStop;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.DuplicateResourceException;
import com.traveleasy.bus.exception.ForbiddenAccessException;
import com.traveleasy.bus.mapper.RouteMapper;
import com.traveleasy.bus.repository.BoardingPointRepository;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.DroppingPointRepository;
import com.traveleasy.bus.repository.RouteRepository;
import com.traveleasy.bus.repository.RouteStopRepository;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.impl.RouteServiceImpl;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteStopRepository routeStopRepository;

    @Mock
    private BoardingPointRepository boardingPointRepository;

    @Mock
    private DroppingPointRepository droppingPointRepository;

    @Mock
    private BusOperatorRepository operatorRepository;

    @Spy
    private RouteMapper routeMapper = new RouteMapper();

    @InjectMocks
    private RouteServiceImpl routeService;

    private UUID operatorId;
    private BusOperator operator;
    private Route route;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        operator = BusOperator.builder()
                .operatorCode("OP-ROUTE-TEST")
                .companyName("Route Test Co")
                .build();
        operator.setId(operatorId);

        route = Route.builder()
                .operator(operator)
                .routeCode("RT-DEL-JAI")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(330)
                .isActive(true)
                .build();
        route.setId(UUID.randomUUID());

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("operator@routes.com")
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
    void shouldCreateRouteSuccessfully() {
        CreateRouteRequest request = CreateRouteRequest.builder()
                .routeCode("RT-DEL-JAI")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(330)
                .build();

        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(routeRepository.existsByOperatorIdAndRouteCode(operatorId, "RT-DEL-JAI")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenReturn(route);

        RouteResponse response = routeService.createRoute(request);

        assertNotNull(response);
        assertEquals("RT-DEL-JAI", response.getRouteCode());
        assertEquals("Delhi", response.getOrigin());
        assertEquals("Jaipur", response.getDestination());
        verify(routeRepository).save(any(Route.class));
    }

    @Test
    void shouldRejectRouteWhenOriginEqualsDestination() {
        CreateRouteRequest request = CreateRouteRequest.builder()
                .routeCode("RT-INVALID")
                .origin("Delhi")
                .destination("delhi") // Identical case-insensitive
                .distanceKm(10.0)
                .estimatedDurationMinutes(30)
                .build();

        assertThrows(BusDomainException.class, () -> routeService.createRoute(request));
        verify(routeRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateRouteCodeForSameOperator() {
        CreateRouteRequest request = CreateRouteRequest.builder()
                .routeCode("RT-DEL-JAI")
                .origin("Delhi")
                .destination("Jaipur")
                .distanceKm(280.0)
                .estimatedDurationMinutes(330)
                .build();

        when(routeRepository.existsByOperatorIdAndRouteCode(operatorId, "RT-DEL-JAI")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> routeService.createRoute(request));
        verify(routeRepository, never()).save(any());
    }

    @Test
    void shouldAddRouteStopSuccessfully() {
        RouteStopRequest stopRequest = RouteStopRequest.builder()
                .stopName("Gurgaon IFFCO Chowk")
                .stopOrder(1)
                .distanceFromOriginKm(30.0)
                .build();

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeStopRepository.existsByRouteIdAndStopOrder(route.getId(), 1)).thenReturn(false);

        RouteStop savedStop = RouteStop.builder()
                .route(route)
                .stopName("Gurgaon IFFCO Chowk")
                .stopOrder(1)
                .distanceFromOriginKm(30.0)
                .build();
        savedStop.setId(UUID.randomUUID());

        when(routeStopRepository.save(any(RouteStop.class))).thenReturn(savedStop);

        RouteStopResponse response = routeService.addRouteStop(route.getId(), stopRequest);

        assertNotNull(response);
        assertEquals("Gurgaon IFFCO Chowk", response.getStopName());
        assertEquals(1, response.getStopOrder());
    }

    @Test
    void shouldRejectStopWhenOrderIsDuplicate() {
        RouteStopRequest stopRequest = RouteStopRequest.builder()
                .stopName("Duplicate Stop")
                .stopOrder(1)
                .distanceFromOriginKm(30.0)
                .build();

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeStopRepository.existsByRouteIdAndStopOrder(route.getId(), 1)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> routeService.addRouteStop(route.getId(), stopRequest));
        verify(routeStopRepository, never()).save(any());
    }

    @Test
    void shouldRejectStopWhenDistanceExceedsRouteTotalDistance() {
        RouteStopRequest stopRequest = RouteStopRequest.builder()
                .stopName("Far Away Stop")
                .stopOrder(5)
                .distanceFromOriginKm(350.0) // Route total is 280.0 km
                .build();

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeStopRepository.existsByRouteIdAndStopOrder(route.getId(), 5)).thenReturn(false);

        assertThrows(BusDomainException.class, () -> routeService.addRouteStop(route.getId(), stopRequest));
    }

    @Test
    void shouldAddBoardingPointSuccessfully() {
        BoardingPointRequest request = BoardingPointRequest.builder()
                .locationName("Kashmere Gate ISBT")
                .landmark("Platform 5")
                .departureOffsetMinutes(0)
                .contactPhone("+919999999999")
                .build();

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));

        BoardingPoint savedPoint = BoardingPoint.builder()
                .route(route)
                .locationName("Kashmere Gate ISBT")
                .departureOffsetMinutes(0)
                .build();
        savedPoint.setId(UUID.randomUUID());

        when(boardingPointRepository.save(any(BoardingPoint.class))).thenReturn(savedPoint);

        BoardingPointResponse response = routeService.addBoardingPoint(route.getId(), request);

        assertNotNull(response);
        assertEquals("Kashmere Gate ISBT", response.getLocationName());
    }

    @Test
    void shouldRejectModificationByDifferentOperator() {
        UUID otherOperatorId = UUID.randomUUID();
        BusOperator otherOperator = BusOperator.builder()
                .operatorCode("OTHER")
                .build();
        otherOperator.setId(otherOperatorId);

        Route otherRoute = Route.builder()
                .operator(otherOperator)
                .routeCode("RT-OTHER")
                .origin("Mumbai")
                .destination("Pune")
                .distanceKm(150.0)
                .estimatedDurationMinutes(180)
                .build();
        otherRoute.setId(UUID.randomUUID());

        when(routeRepository.findById(otherRoute.getId())).thenReturn(Optional.of(otherRoute));

        RouteStopRequest stopRequest = RouteStopRequest.builder()
                .stopName("Navi Mumbai")
                .stopOrder(1)
                .distanceFromOriginKm(25.0)
                .build();

        assertThrows(ForbiddenAccessException.class, () -> routeService.addRouteStop(otherRoute.getId(), stopRequest));
    }
}
