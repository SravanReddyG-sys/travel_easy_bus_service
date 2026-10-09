package com.traveleasy.bus.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorDashboardSummaryResponse {

    @Schema(description = "Operator identifier")
    private UUID operatorId;

    @Schema(description = "Company Name")
    private String companyName;

    // Fleet Stats
    @Schema(description = "Total buses owned by operator")
    private long totalBuses;

    @Schema(description = "Count of ACTIVE buses in service")
    private long activeBuses;

    @Schema(description = "Count of buses currently in MAINTENANCE")
    private long maintenanceBuses;

    @Schema(description = "Count of INACTIVE buses")
    private long inactiveBuses;

    // Route Stats
    @Schema(description = "Total registered routes")
    private long totalRoutes;

    @Schema(description = "Count of active routes")
    private long activeRoutes;

    // Schedule Stats
    @Schema(description = "Total schedules recorded")
    private long totalSchedules;

    @Schema(description = "Schedules scheduled for today")
    private long todaySchedules;

    @Schema(description = "Active upcoming schedules (SCHEDULED + BOARDING)")
    private long activeSchedules;

    @Schema(description = "Completed journeys count")
    private long completedSchedules;

    @Schema(description = "Cancelled schedules count")
    private long cancelledSchedules;

    // Seat & Occupancy Stats
    @Schema(description = "Total seats capacity across all active schedules")
    private long totalSeatsCapacity;

    @Schema(description = "Total booked seats across all schedules")
    private long totalSeatsBooked;

    @Schema(description = "Average fleet occupancy percentage", example = "78.5")
    private Double occupancyRatePercentage;

    // Financial Overview
    @Schema(description = "Total revenue earned from booked seats", example = "125000.00")
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Schema(description = "Currency", example = "INR")
    @Builder.Default
    private String currency = "INR";

    // Recent Schedules
    @Builder.Default
    private List<ScheduleResponse> recentSchedules = new ArrayList<>();
}
