package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.BoardingPointRequest;
import com.traveleasy.bus.dto.request.CreateRouteRequest;
import com.traveleasy.bus.dto.request.DroppingPointRequest;
import com.traveleasy.bus.dto.request.RouteStopRequest;
import com.traveleasy.bus.dto.request.UpdateRouteRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.BoardingPointResponse;
import com.traveleasy.bus.dto.response.DroppingPointResponse;
import com.traveleasy.bus.dto.response.PageResponse;
import com.traveleasy.bus.dto.response.RouteResponse;
import com.traveleasy.bus.dto.response.RouteStopResponse;
import com.traveleasy.bus.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Route Management", description = "APIs for managing routes, stops, boarding points, and dropping points")
public class RouteController {

    private final RouteService routeService;

    // ================= Route Endpoints =================

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create route", description = "Creates a new origin-destination route for the authenticated operator.")
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        RouteResponse response = routeService.createRoute(request);
        return new ResponseEntity<>(ApiResponse.ok("Route created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get route by ID", description = "Retrieves complete route details including stops, boarding, and dropping points.")
    public ResponseEntity<ApiResponse<RouteResponse>> getRouteById(@PathVariable UUID id) {
        RouteResponse response = routeService.getRouteById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List routes", description = "Retrieves paginated routes with optional operator and active filtering.")
    public ResponseEntity<ApiResponse<PageResponse<RouteResponse>>> listRoutes(
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true) Pageable pageable) {
        PageResponse<RouteResponse> response = routeService.listRoutes(operatorId, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update route", description = "Updates details of an existing route owned by the operator.")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRouteRequest request) {
        RouteResponse response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Route updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Deactivate/delete route", description = "Soft-deletes or deactivates an existing route.")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable UUID id) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.ok("Route deactivated successfully", null));
    }

    // ================= Route Stop Endpoints =================

    @PostMapping("/{routeId}/stops")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Add route stop", description = "Adds an intermediate stop along the route.")
    public ResponseEntity<ApiResponse<RouteStopResponse>> addRouteStop(
            @PathVariable UUID routeId,
            @Valid @RequestBody RouteStopRequest request) {
        RouteStopResponse response = routeService.addRouteStop(routeId, request);
        return new ResponseEntity<>(ApiResponse.ok("Route stop added successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{routeId}/stops")
    @Operation(summary = "Get route stops", description = "Retrieves all stops for the route in sequential order.")
    public ResponseEntity<ApiResponse<List<RouteStopResponse>>> getRouteStops(@PathVariable UUID routeId) {
        List<RouteStopResponse> response = routeService.getRouteStops(routeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{routeId}/stops/{stopId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update route stop", description = "Updates an intermediate stop's details and sequence order.")
    public ResponseEntity<ApiResponse<RouteStopResponse>> updateRouteStop(
            @PathVariable UUID routeId,
            @PathVariable UUID stopId,
            @Valid @RequestBody RouteStopRequest request) {
        RouteStopResponse response = routeService.updateRouteStop(routeId, stopId, request);
        return ResponseEntity.ok(ApiResponse.ok("Route stop updated successfully", response));
    }

    @DeleteMapping("/{routeId}/stops/{stopId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete route stop", description = "Removes an intermediate stop from the route.")
    public ResponseEntity<ApiResponse<Void>> deleteRouteStop(
            @PathVariable UUID routeId,
            @PathVariable UUID stopId) {
        routeService.deleteRouteStop(routeId, stopId);
        return ResponseEntity.ok(ApiResponse.ok("Route stop deleted successfully", null));
    }

    // ================= Boarding Point Endpoints =================

    @PostMapping("/{routeId}/boarding-points")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Add boarding point", description = "Adds a passenger pickup location on the route.")
    public ResponseEntity<ApiResponse<BoardingPointResponse>> addBoardingPoint(
            @PathVariable UUID routeId,
            @Valid @RequestBody BoardingPointRequest request) {
        BoardingPointResponse response = routeService.addBoardingPoint(routeId, request);
        return new ResponseEntity<>(ApiResponse.ok("Boarding point added successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{routeId}/boarding-points")
    @Operation(summary = "Get boarding points", description = "Lists all boarding locations for the route ordered by departure time offset.")
    public ResponseEntity<ApiResponse<List<BoardingPointResponse>>> getBoardingPoints(@PathVariable UUID routeId) {
        List<BoardingPointResponse> response = routeService.getBoardingPoints(routeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{routeId}/boarding-points/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update boarding point", description = "Updates boarding location and time offset.")
    public ResponseEntity<ApiResponse<BoardingPointResponse>> updateBoardingPoint(
            @PathVariable UUID routeId,
            @PathVariable UUID id,
            @Valid @RequestBody BoardingPointRequest request) {
        BoardingPointResponse response = routeService.updateBoardingPoint(routeId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Boarding point updated successfully", response));
    }

    @DeleteMapping("/{routeId}/boarding-points/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete boarding point", description = "Removes a boarding location from the route.")
    public ResponseEntity<ApiResponse<Void>> deleteBoardingPoint(
            @PathVariable UUID routeId,
            @PathVariable UUID id) {
        routeService.deleteBoardingPoint(routeId, id);
        return ResponseEntity.ok(ApiResponse.ok("Boarding point deleted successfully", null));
    }

    // ================= Dropping Point Endpoints =================

    @PostMapping("/{routeId}/dropping-points")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Add dropping point", description = "Adds a passenger dropoff location on the route.")
    public ResponseEntity<ApiResponse<DroppingPointResponse>> addDroppingPoint(
            @PathVariable UUID routeId,
            @Valid @RequestBody DroppingPointRequest request) {
        DroppingPointResponse response = routeService.addDroppingPoint(routeId, request);
        return new ResponseEntity<>(ApiResponse.ok("Dropping point added successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{routeId}/dropping-points")
    @Operation(summary = "Get dropping points", description = "Lists all dropoff locations for the route ordered by arrival time offset.")
    public ResponseEntity<ApiResponse<List<DroppingPointResponse>>> getDroppingPoints(@PathVariable UUID routeId) {
        List<DroppingPointResponse> response = routeService.getDroppingPoints(routeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{routeId}/dropping-points/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update dropping point", description = "Updates dropoff location and time offset.")
    public ResponseEntity<ApiResponse<DroppingPointResponse>> updateDroppingPoint(
            @PathVariable UUID routeId,
            @PathVariable UUID id,
            @Valid @RequestBody DroppingPointRequest request) {
        DroppingPointResponse response = routeService.updateDroppingPoint(routeId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Dropping point updated successfully", response));
    }

    @DeleteMapping("/{routeId}/dropping-points/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete dropping point", description = "Removes a dropoff location from the route.")
    public ResponseEntity<ApiResponse<Void>> deleteDroppingPoint(
            @PathVariable UUID routeId,
            @PathVariable UUID id) {
        routeService.deleteDroppingPoint(routeId, id);
        return ResponseEntity.ok(ApiResponse.ok("Dropping point deleted successfully", null));
    }
}
