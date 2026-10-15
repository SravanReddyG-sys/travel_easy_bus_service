package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.BoardingPointRequest;
import com.traveleasy.bus.dto.request.CreateRouteRequest;
import com.traveleasy.bus.dto.request.DroppingPointRequest;
import com.traveleasy.bus.dto.request.RouteStopRequest;
import com.traveleasy.bus.dto.request.UpdateRouteRequest;
import com.traveleasy.bus.dto.response.BoardingPointResponse;
import com.traveleasy.bus.dto.response.DroppingPointResponse;
import com.traveleasy.bus.dto.response.PageResponse;
import com.traveleasy.bus.dto.response.RouteResponse;
import com.traveleasy.bus.dto.response.RouteStopResponse;
import com.traveleasy.bus.entity.BoardingPoint;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.DroppingPoint;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.RouteStop;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.DuplicateResourceException;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.RouteMapper;
import com.traveleasy.bus.repository.BoardingPointRepository;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.DroppingPointRepository;
import com.traveleasy.bus.repository.RouteRepository;
import com.traveleasy.bus.repository.RouteStopRepository;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final BoardingPointRepository boardingPointRepository;
    private final DroppingPointRepository droppingPointRepository;
    private final BusOperatorRepository operatorRepository;
    private final RouteMapper routeMapper;

    @Override
    @Transactional
    public RouteResponse createRoute(CreateRouteRequest request) {
        UUID operatorId = SecurityUtils.requireCurrentOperatorId();

        // Validate Origin != Destination
        validateOriginAndDestination(request.getOrigin(), request.getDestination());

        // Validate Route code uniqueness for this operator
        if (routeRepository.existsByOperatorIdAndRouteCode(operatorId, request.getRouteCode().trim().toUpperCase())) {
            throw new DuplicateResourceException("Route", "routeCode", request.getRouteCode());
        }

        BusOperator operator = operatorRepository.findById(operatorId)
                .orElseGet(() -> {
                    UserPrincipal principal = SecurityUtils.requireCurrentUserPrincipal();
                    BusOperator newOperator = BusOperator.builder()
                            .operatorCode("OP-" + operatorId.toString().substring(0, 8).toUpperCase())
                            .companyName(principal.getEmail() != null ? principal.getEmail() : "Operator " + operatorId)
                            .contactEmail(principal.getEmail() != null ? principal.getEmail() : "operator@traveleasy.com")
                            .contactPhone("+910000000000")
                            .isActive(true)
                            .build();
                    newOperator.setId(operatorId);
                    return operatorRepository.save(newOperator);
                });

        Route route = routeMapper.toEntity(request, operator);
        route = routeRepository.save(route);
        log.info("Created route [{}] from [{}] to [{}] with id [{}]",
                route.getRouteCode(), route.getOrigin(), route.getDestination(), route.getId());

        return routeMapper.toResponse(route);
    }

    @Override
    public RouteResponse getRouteById(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
        return routeMapper.toResponse(route);
    }

    @Override
    public PageResponse<RouteResponse> listRoutes(UUID operatorIdFilter, Boolean activeOnly, Pageable pageable) {
        Page<Route> routePage;
        if (operatorIdFilter != null) {
            routePage = routeRepository.findByOperatorId(operatorIdFilter, pageable);
        } else {
            routePage = routeRepository.findAll(pageable);
        }

        return PageResponse.from(routePage.map(routeMapper::toResponse));
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(UUID routeId, UpdateRouteRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());
        validateOriginAndDestination(request.getOrigin(), request.getDestination());

        if (!route.getRouteCode().equalsIgnoreCase(request.getRouteCode().trim()) &&
                routeRepository.existsByOperatorIdAndRouteCode(route.getOperator().getId(), request.getRouteCode().trim().toUpperCase())) {
            throw new DuplicateResourceException("Route", "routeCode", request.getRouteCode());
        }

        routeMapper.updateEntityFromDto(request, route);
        route = routeRepository.save(route);
        log.info("Updated route [{}] with id [{}]", route.getRouteCode(), route.getId());

        return routeMapper.toResponse(route);
    }

    @Override
    @Transactional
    public void deleteRoute(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        route.setIsActive(false);
        routeRepository.save(route);
        log.info("Deactivated route [{}]", routeId);
    }

    // ================= Route Stops =================

    @Override
    @Transactional
    public RouteStopResponse addRouteStop(UUID routeId, RouteStopRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        if (routeStopRepository.existsByRouteIdAndStopOrder(routeId, request.getStopOrder())) {
            throw new DuplicateResourceException("RouteStop", "stopOrder", request.getStopOrder());
        }

        if (request.getDistanceFromOriginKm() > route.getDistanceKm()) {
            throw new BusDomainException(ErrorCode.ROUTE_INVALID,
                    String.format("Stop distance (%f km) cannot exceed total route distance (%f km)",
                            request.getDistanceFromOriginKm(), route.getDistanceKm()));
        }

        RouteStop stop = routeMapper.toStopEntity(request, route);
        stop = routeStopRepository.save(stop);
        log.info("Added stop [{}] to route [{}] with order [{}]", stop.getStopName(), routeId, stop.getStopOrder());

        return routeMapper.toStopResponse(stop);
    }

    @Override
    public List<RouteStopResponse> getRouteStops(UUID routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route", routeId);
        }
        return routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId)
                .stream()
                .map(routeMapper::toStopResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RouteStopResponse updateRouteStop(UUID routeId, UUID stopId, RouteStopRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        RouteStop stop = routeStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("RouteStop", stopId));

        if (!stop.getRoute().getId().equals(routeId)) {
            throw new ResourceNotFoundException("RouteStop with ID " + stopId + " does not belong to Route " + routeId);
        }

        if (!stop.getStopOrder().equals(request.getStopOrder()) &&
                routeStopRepository.existsByRouteIdAndStopOrder(routeId, request.getStopOrder())) {
            throw new DuplicateResourceException("RouteStop", "stopOrder", request.getStopOrder());
        }

        stop.setStopName(request.getStopName().trim());
        stop.setStopOrder(request.getStopOrder());
        stop.setDistanceFromOriginKm(request.getDistanceFromOriginKm());

        stop = routeStopRepository.save(stop);
        return routeMapper.toStopResponse(stop);
    }

    @Override
    @Transactional
    public void deleteRouteStop(UUID routeId, UUID stopId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        RouteStop stop = routeStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("RouteStop", stopId));

        if (!stop.getRoute().getId().equals(routeId)) {
            throw new ResourceNotFoundException("RouteStop with ID " + stopId + " does not belong to Route " + routeId);
        }

        routeStopRepository.delete(stop);
        log.info("Deleted route stop [{}] from route [{}]", stopId, routeId);
    }

    // ================= Boarding Points =================

    @Override
    @Transactional
    public BoardingPointResponse addBoardingPoint(UUID routeId, BoardingPointRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        RouteStop stop = null;
        if (request.getStopId() != null) {
            stop = routeStopRepository.findById(request.getStopId())
                    .orElseThrow(() -> new ResourceNotFoundException("RouteStop", request.getStopId()));
            if (!stop.getRoute().getId().equals(routeId)) {
                throw new BusDomainException(ErrorCode.ROUTE_INVALID, "Specified Stop does not belong to Route " + routeId);
            }
        }

        BoardingPoint point = routeMapper.toBoardingEntity(request, route, stop);
        point = boardingPointRepository.save(point);
        log.info("Added boarding point [{}] to route [{}]", point.getLocationName(), routeId);

        return routeMapper.toBoardingResponse(point);
    }

    @Override
    public List<BoardingPointResponse> getBoardingPoints(UUID routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route", routeId);
        }
        return boardingPointRepository.findByRouteIdOrderByDepartureOffsetMinutesAsc(routeId)
                .stream()
                .map(routeMapper::toBoardingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BoardingPointResponse updateBoardingPoint(UUID routeId, UUID pointId, BoardingPointRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        BoardingPoint point = boardingPointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardingPoint", pointId));

        if (!point.getRoute().getId().equals(routeId)) {
            throw new ResourceNotFoundException("BoardingPoint does not belong to Route " + routeId);
        }

        RouteStop stop = null;
        if (request.getStopId() != null) {
            stop = routeStopRepository.findById(request.getStopId())
                    .orElseThrow(() -> new ResourceNotFoundException("RouteStop", request.getStopId()));
            if (!stop.getRoute().getId().equals(routeId)) {
                throw new BusDomainException(ErrorCode.ROUTE_INVALID, "Specified Stop does not belong to Route " + routeId);
            }
        }

        point.setStop(stop);
        point.setLocationName(request.getLocationName().trim());
        point.setLandmark(request.getLandmark() != null ? request.getLandmark().trim() : null);
        point.setDepartureOffsetMinutes(request.getDepartureOffsetMinutes());
        point.setContactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null);

        point = boardingPointRepository.save(point);
        return routeMapper.toBoardingResponse(point);
    }

    @Override
    @Transactional
    public void deleteBoardingPoint(UUID routeId, UUID pointId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        BoardingPoint point = boardingPointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardingPoint", pointId));

        if (!point.getRoute().getId().equals(routeId)) {
            throw new ResourceNotFoundException("BoardingPoint does not belong to Route " + routeId);
        }

        boardingPointRepository.delete(point);
        log.info("Deleted boarding point [{}] from route [{}]", pointId, routeId);
    }

    // ================= Dropping Points =================

    @Override
    @Transactional
    public DroppingPointResponse addDroppingPoint(UUID routeId, DroppingPointRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        RouteStop stop = null;
        if (request.getStopId() != null) {
            stop = routeStopRepository.findById(request.getStopId())
                    .orElseThrow(() -> new ResourceNotFoundException("RouteStop", request.getStopId()));
            if (!stop.getRoute().getId().equals(routeId)) {
                throw new BusDomainException(ErrorCode.ROUTE_INVALID, "Specified Stop does not belong to Route " + routeId);
            }
        }

        DroppingPoint point = routeMapper.toDroppingEntity(request, route, stop);
        point = droppingPointRepository.save(point);
        log.info("Added dropping point [{}] to route [{}]", point.getLocationName(), routeId);

        return routeMapper.toDroppingResponse(point);
    }

    @Override
    public List<DroppingPointResponse> getDroppingPoints(UUID routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route", routeId);
        }
        return droppingPointRepository.findByRouteIdOrderByArrivalOffsetMinutesAsc(routeId)
                .stream()
                .map(routeMapper::toDroppingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DroppingPointResponse updateDroppingPoint(UUID routeId, UUID pointId, DroppingPointRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        DroppingPoint point = droppingPointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("DroppingPoint", pointId));

        if (!point.getRoute().getId().equals(routeId)) {
            throw new ResourceNotFoundException("DroppingPoint does not belong to Route " + routeId);
        }

        RouteStop stop = null;
        if (request.getStopId() != null) {
            stop = routeStopRepository.findById(request.getStopId())
                    .orElseThrow(() -> new ResourceNotFoundException("RouteStop", request.getStopId()));
            if (!stop.getRoute().getId().equals(routeId)) {
                throw new BusDomainException(ErrorCode.ROUTE_INVALID, "Specified Stop does not belong to Route " + routeId);
            }
        }

        point.setStop(stop);
        point.setLocationName(request.getLocationName().trim());
        point.setLandmark(request.getLandmark() != null ? request.getLandmark().trim() : null);
        point.setArrivalOffsetMinutes(request.getArrivalOffsetMinutes());
        point.setContactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null);

        point = droppingPointRepository.save(point);
        return routeMapper.toDroppingResponse(point);
    }

    @Override
    @Transactional
    public void deleteDroppingPoint(UUID routeId, UUID pointId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        DroppingPoint point = droppingPointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("DroppingPoint", pointId));

        if (!point.getRoute().getId().equals(routeId)) {
            throw new ResourceNotFoundException("DroppingPoint does not belong to Route " + routeId);
        }

        droppingPointRepository.delete(point);
        log.info("Deleted dropping point [{}] from route [{}]", pointId, routeId);
    }

    private void validateOriginAndDestination(String origin, String destination) {
        if (origin != null && destination != null && origin.trim().equalsIgnoreCase(destination.trim())) {
            throw new BusDomainException(ErrorCode.ROUTE_INVALID, "Origin and Destination cannot be the same location");
        }
    }
}
