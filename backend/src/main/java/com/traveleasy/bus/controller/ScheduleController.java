package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.CreateScheduleRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.PageResponse;
import com.traveleasy.bus.dto.response.ScheduleResponse;
import com.traveleasy.bus.dto.response.TravellerScheduleDetailResponse;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule Management", description = "APIs for bus schedule lifecycle, timing, search, and inventory initialization")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create schedule", description = "Creates a new bus schedule and initializes seat inventory.")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return new ResponseEntity<>(ApiResponse.ok("Schedule created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID", description = "Retrieves schedule details along with current available seats count.")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleById(@PathVariable UUID id) {
        ScheduleResponse response = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Get full traveller schedule details",
               description = "Public endpoint providing deep schedule view: route stops, boarding/dropping points, pricing tiers, amenities, terms, and live interactive seat map.")
    public ResponseEntity<ApiResponse<TravellerScheduleDetailResponse>> getTravellerScheduleDetails(@PathVariable UUID id) {
        TravellerScheduleDetailResponse response = scheduleService.getTravellerScheduleDetails(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List/search schedules", description = "Retrieves paginated schedules with optional filters.")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleResponse>>> getSchedules(
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(required = false) UUID busId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate,
            @RequestParam(required = false) ScheduleStatus status,
            @RequestParam(required = false) AvailabilityStatus availabilityStatus,
            @PageableDefault(page = 0, size = 20, sort = "departureTime", direction = Sort.Direction.ASC)
            @Parameter(hidden = true) Pageable pageable) {

        PageResponse<ScheduleResponse> response = scheduleService.getSchedules(
                operatorId, routeId, busId, scheduleDate, status, availabilityStatus, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/operator/my-schedules")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get operator schedules", description = "Retrieves paginated schedules owned by the authenticated operator.")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleResponse>>> getOperatorSchedules(
            @PageableDefault(page = 0, size = 20, sort = "departureTime", direction = Sort.Direction.ASC)
            @Parameter(hidden = true) Pageable pageable) {

        PageResponse<ScheduleResponse> response = scheduleService.getOperatorSchedules(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update schedule", description = "Updates details, timing, or status of an existing schedule.")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScheduleRequest request) {

        ScheduleResponse response = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Schedule updated successfully", response));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Cancel schedule", description = "Cancels a schedule with a reason.")
    public ResponseEntity<ApiResponse<ScheduleResponse>> cancelSchedule(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Cancelled by operator") String reason) {

        ScheduleResponse response = scheduleService.cancelSchedule(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Schedule cancelled successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete schedule", description = "Permanently deletes a schedule.")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable UUID id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok("Schedule deleted successfully", null));
    }
}
