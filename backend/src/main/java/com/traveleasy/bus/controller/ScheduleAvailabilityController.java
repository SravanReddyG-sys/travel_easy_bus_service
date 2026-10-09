package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.UpdateScheduleAvailabilityRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleStatusRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.ScheduleAvailabilityHistoryResponse;
import com.traveleasy.bus.dto.response.ScheduleResponse;
import com.traveleasy.bus.service.ScheduleAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules/{id}")
@RequiredArgsConstructor
@Tag(name = "Schedule Availability & Lifecycle", description = "APIs for toggling booking availability and tracking schedule lifecycle state transitions")
public class ScheduleAvailabilityController {

    private final ScheduleAvailabilityService availabilityService;

    @PutMapping("/availability")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update schedule availability", description = "Toggles schedule availability (AVAILABLE/UNAVAILABLE) and logs an audit record.")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateAvailability(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScheduleAvailabilityRequest request) {

        ScheduleResponse response = availabilityService.updateScheduleAvailability(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Schedule availability updated successfully", response));
    }

    @PatchMapping("/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update schedule lifecycle status", description = "Transitions schedule status through SCHEDULED -> BOARDING -> DEPARTED -> COMPLETED / CANCELLED.")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScheduleStatusRequest request) {

        ScheduleResponse response = availabilityService.updateScheduleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Schedule status updated successfully", response));
    }

    @GetMapping("/availability/history")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get schedule availability audit history", description = "Retrieves chronological log of all availability changes for this schedule.")
    public ResponseEntity<ApiResponse<List<ScheduleAvailabilityHistoryResponse>>> getAvailabilityHistory(@PathVariable UUID id) {
        List<ScheduleAvailabilityHistoryResponse> response = availabilityService.getScheduleAvailabilityHistory(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
