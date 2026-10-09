package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.response.OperatorDashboardSummaryResponse;
import com.traveleasy.bus.dto.response.OperatorLiveBookingFeedResponse;
import com.traveleasy.bus.dto.response.OperatorOccupancyStatResponse;
import com.traveleasy.bus.dto.response.OperatorRevenueStatResponse;
import com.traveleasy.bus.dto.response.SchedulePassengerManifestResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OperatorDashboardService {

    /**
     * Retrieves high-level operational KPIs, fleet status breakdown, occupancy metrics,
     * total revenue, and recent schedules for the operator.
     */
    OperatorDashboardSummaryResponse getDashboardSummary(UUID operatorId);

    /**
     * Retrieves aggregated revenue and booking analytics broken down by route.
     */
    List<OperatorRevenueStatResponse> getRevenueByRoute(UUID operatorId);

    /**
     * Retrieves detailed trip occupancy and seat utilization analytics across schedules.
     */
    List<OperatorOccupancyStatResponse> getOccupancyAnalytics(UUID operatorId, LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves the complete passenger manifest & live seat roster for a specific schedule.
     */
    SchedulePassengerManifestResponse getScheduleManifest(UUID scheduleId);

    /**
     * Retrieves real-time booking and lock activity feed across operator schedules.
     */
    List<OperatorLiveBookingFeedResponse> getLiveBookingFeed(UUID operatorId, int limit);
}
