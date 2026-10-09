package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.response.*;
import com.traveleasy.bus.service.OperatorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operator/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Operator Dashboard & Analytics", description = "Fleet operational KPIs, revenue breakdowns, occupancy metrics and live manifests")
public class OperatorDashboardController {

    private final OperatorDashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get operator dashboard overview",
               description = "Returns aggregate KPIs for fleet buses, routes, schedules, occupancy percentage, and total revenue.")
    public ResponseEntity<ApiResponse<OperatorDashboardSummaryResponse>> getSummary(
            @RequestParam(required = false) UUID operatorId) {
        OperatorDashboardSummaryResponse response = dashboardService.getDashboardSummary(operatorId);
        return ResponseEntity.ok(ApiResponse.ok("Dashboard summary retrieved successfully", response));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get route revenue analytics",
               description = "Returns revenue and seat bookings broken down per route for the operator.")
    public ResponseEntity<ApiResponse<List<OperatorRevenueStatResponse>>> getRevenueByRoute(
            @RequestParam(required = false) UUID operatorId) {
        List<OperatorRevenueStatResponse> response = dashboardService.getRevenueByRoute(operatorId);
        return ResponseEntity.ok(ApiResponse.ok("Route revenue analytics retrieved successfully", response));
    }

    @GetMapping("/occupancy")
    @Operation(summary = "Get schedule occupancy analytics",
               description = "Returns real-time occupancy rates and seat fill stats across operator schedules.")
    public ResponseEntity<ApiResponse<List<OperatorOccupancyStatResponse>>> getOccupancy(
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OperatorOccupancyStatResponse> response = dashboardService.getOccupancyAnalytics(operatorId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Occupancy analytics retrieved successfully", response));
    }

    @GetMapping("/schedules/{scheduleId}/manifest")
    @Operation(summary = "Get live schedule passenger manifest and seat roster",
               description = "Returns live seat booking details, passenger references, locks, and revenue for a schedule.")
    public ResponseEntity<ApiResponse<SchedulePassengerManifestResponse>> getScheduleManifest(
            @PathVariable UUID scheduleId) {
        SchedulePassengerManifestResponse response = dashboardService.getScheduleManifest(scheduleId);
        return ResponseEntity.ok(ApiResponse.ok("Schedule passenger manifest retrieved successfully", response));
    }

    @GetMapping("/feed/live-bookings")
    @Operation(summary = "Get real-time booking and lock activity feed",
               description = "Returns real-time seat locks and booking activity across the operator's trips.")
    public ResponseEntity<ApiResponse<List<OperatorLiveBookingFeedResponse>>> getLiveBookingFeed(
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(defaultValue = "20") int limit) {
        List<OperatorLiveBookingFeedResponse> response = dashboardService.getLiveBookingFeed(operatorId, limit);
        return ResponseEntity.ok(ApiResponse.ok("Live booking activity feed retrieved successfully", response));
    }
}
