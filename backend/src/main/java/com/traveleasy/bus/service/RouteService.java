package com.traveleasy.bus.service;

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
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RouteService {
    // Route Operations
    RouteResponse createRoute(CreateRouteRequest request);
    RouteResponse getRouteById(UUID routeId);
    PageResponse<RouteResponse> listRoutes(UUID operatorIdFilter, Boolean activeOnly, Pageable pageable);
    RouteResponse updateRoute(UUID routeId, UpdateRouteRequest request);
    void deleteRoute(UUID routeId);

    // Route Stop Operations
    RouteStopResponse addRouteStop(UUID routeId, RouteStopRequest request);
    List<RouteStopResponse> getRouteStops(UUID routeId);
    RouteStopResponse updateRouteStop(UUID routeId, UUID stopId, RouteStopRequest request);
    void deleteRouteStop(UUID routeId, UUID stopId);

    // Boarding Point Operations
    BoardingPointResponse addBoardingPoint(UUID routeId, BoardingPointRequest request);
    List<BoardingPointResponse> getBoardingPoints(UUID routeId);
    BoardingPointResponse updateBoardingPoint(UUID routeId, UUID pointId, BoardingPointRequest request);
    void deleteBoardingPoint(UUID routeId, UUID pointId);

    // Dropping Point Operations
    DroppingPointResponse addDroppingPoint(UUID routeId, DroppingPointRequest request);
    List<DroppingPointResponse> getDroppingPoints(UUID routeId);
    DroppingPointResponse updateDroppingPoint(UUID routeId, UUID pointId, DroppingPointRequest request);
    void deleteDroppingPoint(UUID routeId, UUID pointId);
}
