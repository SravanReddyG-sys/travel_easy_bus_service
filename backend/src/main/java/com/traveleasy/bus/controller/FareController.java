package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.CreateFareRequest;
import com.traveleasy.bus.dto.request.UpdateFareRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.FareResponse;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules/{scheduleId}/fares")
@RequiredArgsConstructor
@Tag(name = "Fare Management", description = "APIs for setting, updating, and querying schedule seat-type pricing and fares")
public class FareController {

    private final FareService fareService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Set fare for seat type", description = "Sets or overrides the price for a specific seat type on the schedule.")
    public ResponseEntity<ApiResponse<FareResponse>> setFareForSeatType(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody CreateFareRequest request) {

        FareResponse response = fareService.setFareForSeatType(scheduleId, request);
        return new ResponseEntity<>(ApiResponse.ok("Fare configured successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/bulk")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Set bulk fares", description = "Sets prices for multiple seat types on the schedule in a single batch.")
    public ResponseEntity<ApiResponse<List<FareResponse>>> setFaresBulk(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody List<CreateFareRequest> requests) {

        List<FareResponse> response = fareService.setFaresBulk(scheduleId, requests);
        return ResponseEntity.ok(ApiResponse.ok("Fares configured successfully in bulk", response));
    }

    @GetMapping
    @Operation(summary = "Get schedule fares", description = "Retrieves all configured seat prices and fares for a schedule.")
    public ResponseEntity<ApiResponse<List<FareResponse>>> getFaresByScheduleId(@PathVariable UUID scheduleId) {
        List<FareResponse> response = fareService.getFaresByScheduleId(scheduleId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{seatType}")
    @Operation(summary = "Get fare by seat type", description = "Retrieves configured fare for a specific seat type on the schedule.")
    public ResponseEntity<ApiResponse<FareResponse>> getFareByScheduleAndSeatType(
            @PathVariable UUID scheduleId,
            @PathVariable SeatType seatType) {

        FareResponse response = fareService.getFareByScheduleAndSeatType(scheduleId, seatType);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{fareId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update fare", description = "Updates pricing, tax, or discount for an existing schedule fare.")
    public ResponseEntity<ApiResponse<FareResponse>> updateFare(
            @PathVariable UUID scheduleId,
            @PathVariable UUID fareId,
            @Valid @RequestBody UpdateFareRequest request) {

        FareResponse response = fareService.updateFare(scheduleId, fareId, request);
        return ResponseEntity.ok(ApiResponse.ok("Fare updated successfully", response));
    }

    @DeleteMapping("/{fareId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete fare", description = "Removes a configured fare from the schedule.")
    public ResponseEntity<ApiResponse<Void>> deleteFare(
            @PathVariable UUID scheduleId,
            @PathVariable UUID fareId) {

        fareService.deleteFare(scheduleId, fareId);
        return ResponseEntity.ok(ApiResponse.ok("Fare deleted successfully", null));
    }
}
