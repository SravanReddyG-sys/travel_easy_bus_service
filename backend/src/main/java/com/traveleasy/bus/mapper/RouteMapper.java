package com.traveleasy.bus.mapper;

import com.traveleasy.bus.dto.request.BoardingPointRequest;
import com.traveleasy.bus.dto.request.CreateRouteRequest;
import com.traveleasy.bus.dto.request.DroppingPointRequest;
import com.traveleasy.bus.dto.request.RouteStopRequest;
import com.traveleasy.bus.dto.request.UpdateRouteRequest;
import com.traveleasy.bus.dto.response.BoardingPointResponse;
import com.traveleasy.bus.dto.response.DroppingPointResponse;
import com.traveleasy.bus.dto.response.RouteResponse;
import com.traveleasy.bus.dto.response.RouteStopResponse;
import com.traveleasy.bus.entity.BoardingPoint;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.DroppingPoint;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.RouteStop;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RouteMapper {

    public Route toEntity(CreateRouteRequest request, BusOperator operator) {
        if (request == null) {
            return null;
        }

        return Route.builder()
                .operator(operator)
                .routeCode(request.getRouteCode().trim().toUpperCase())
                .origin(request.getOrigin().trim())
                .destination(request.getDestination().trim())
                .distanceKm(request.getDistanceKm())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .isActive(true)
                .stops(new ArrayList<>())
                .boardingPoints(new ArrayList<>())
                .droppingPoints(new ArrayList<>())
                .build();
    }

    public void updateEntityFromDto(UpdateRouteRequest request, Route route) {
        if (request == null || route == null) {
            return;
        }

        route.setRouteCode(request.getRouteCode().trim().toUpperCase());
        route.setOrigin(request.getOrigin().trim());
        route.setDestination(request.getDestination().trim());
        route.setDistanceKm(request.getDistanceKm());
        route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        if (request.getIsActive() != null) {
            route.setIsActive(request.getIsActive());
        }
    }

    public RouteStop toStopEntity(RouteStopRequest request, Route route) {
        if (request == null) {
            return null;
        }

        return RouteStop.builder()
                .route(route)
                .stopName(request.getStopName().trim())
                .stopOrder(request.getStopOrder())
                .distanceFromOriginKm(request.getDistanceFromOriginKm())
                .build();
    }

    public BoardingPoint toBoardingEntity(BoardingPointRequest request, Route route, RouteStop stop) {
        if (request == null) {
            return null;
        }

        return BoardingPoint.builder()
                .route(route)
                .stop(stop)
                .locationName(request.getLocationName().trim())
                .landmark(request.getLandmark() != null ? request.getLandmark().trim() : null)
                .departureOffsetMinutes(request.getDepartureOffsetMinutes())
                .contactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null)
                .build();
    }

    public DroppingPoint toDroppingEntity(DroppingPointRequest request, Route route, RouteStop stop) {
        if (request == null) {
            return null;
        }

        return DroppingPoint.builder()
                .route(route)
                .stop(stop)
                .locationName(request.getLocationName().trim())
                .landmark(request.getLandmark() != null ? request.getLandmark().trim() : null)
                .arrivalOffsetMinutes(request.getArrivalOffsetMinutes())
                .contactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null)
                .build();
    }

    public RouteStopResponse toStopResponse(RouteStop stop) {
        if (stop == null) {
            return null;
        }

        return RouteStopResponse.builder()
                .id(stop.getId())
                .routeId(stop.getRoute() != null ? stop.getRoute().getId() : null)
                .stopName(stop.getStopName())
                .stopOrder(stop.getStopOrder())
                .distanceFromOriginKm(stop.getDistanceFromOriginKm())
                .build();
    }

    public BoardingPointResponse toBoardingResponse(BoardingPoint point) {
        if (point == null) {
            return null;
        }

        return BoardingPointResponse.builder()
                .id(point.getId())
                .routeId(point.getRoute() != null ? point.getRoute().getId() : null)
                .stopId(point.getStop() != null ? point.getStop().getId() : null)
                .stopName(point.getStop() != null ? point.getStop().getStopName() : null)
                .locationName(point.getLocationName())
                .landmark(point.getLandmark())
                .departureOffsetMinutes(point.getDepartureOffsetMinutes())
                .contactPhone(point.getContactPhone())
                .build();
    }

    public DroppingPointResponse toDroppingResponse(DroppingPoint point) {
        if (point == null) {
            return null;
        }

        return DroppingPointResponse.builder()
                .id(point.getId())
                .routeId(point.getRoute() != null ? point.getRoute().getId() : null)
                .stopId(point.getStop() != null ? point.getStop().getId() : null)
                .stopName(point.getStop() != null ? point.getStop().getStopName() : null)
                .locationName(point.getLocationName())
                .landmark(point.getLandmark())
                .arrivalOffsetMinutes(point.getArrivalOffsetMinutes())
                .contactPhone(point.getContactPhone())
                .build();
    }

    public RouteResponse toResponse(Route route) {
        if (route == null) {
            return null;
        }

        List<RouteStopResponse> stops = route.getStops() != null ?
                route.getStops().stream().map(this::toStopResponse).collect(Collectors.toList()) :
                new ArrayList<>();

        List<BoardingPointResponse> boarding = route.getBoardingPoints() != null ?
                route.getBoardingPoints().stream().map(this::toBoardingResponse).collect(Collectors.toList()) :
                new ArrayList<>();

        List<DroppingPointResponse> dropping = route.getDroppingPoints() != null ?
                route.getDroppingPoints().stream().map(this::toDroppingResponse).collect(Collectors.toList()) :
                new ArrayList<>();

        return RouteResponse.builder()
                .id(route.getId())
                .operatorId(route.getOperator() != null ? route.getOperator().getId() : null)
                .operatorCompanyName(route.getOperator() != null ? route.getOperator().getCompanyName() : null)
                .operatorCode(route.getOperator() != null ? route.getOperator().getOperatorCode() : null)
                .routeCode(route.getRouteCode())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .isActive(route.getIsActive())
                .stops(stops)
                .boardingPoints(boarding)
                .droppingPoints(dropping)
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
