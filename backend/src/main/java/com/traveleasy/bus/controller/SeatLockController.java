package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.ConfirmSeatsRequest;
import com.traveleasy.bus.dto.request.LockSeatsRequest;
import com.traveleasy.bus.dto.request.ReleaseSeatsRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.ConfirmSeatsResponse;
import com.traveleasy.bus.dto.response.LockSeatsResponse;
import com.traveleasy.bus.dto.response.ReleaseSeatsResponse;
import com.traveleasy.bus.service.SeatLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules/{scheduleId}/seats")
@RequiredArgsConstructor
@Tag(name = "Seat Locking & Booking Concurrency", description = "High-concurrency APIs for locking seats with TTL, confirming bookings, and releasing locks")
public class SeatLockController {

    private final SeatLockService seatLockService;

    @PostMapping("/lock")
    @Operation(summary = "Lock seats with TTL", description = "Atomically locks requested seats with row-level pessimistic write lock and TTL. Returns lock token.")
    public ResponseEntity<ApiResponse<LockSeatsResponse>> lockSeats(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody LockSeatsRequest request) {

        LockSeatsResponse response = seatLockService.lockSeats(scheduleId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seats locked successfully", response));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm locked seats", description = "Finalizes booking confirmation, moving locked seats to BOOKED state with booking reference.")
    public ResponseEntity<ApiResponse<ConfirmSeatsResponse>> confirmSeats(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody ConfirmSeatsRequest request) {

        ConfirmSeatsResponse response = seatLockService.confirmSeats(scheduleId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seats confirmed and booked successfully", response));
    }

    @PostMapping("/release")
    @Operation(summary = "Release seats", description = "Releases temporarily locked or cancelled seats back to AVAILABLE state.")
    public ResponseEntity<ApiResponse<ReleaseSeatsResponse>> releaseSeats(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody ReleaseSeatsRequest request) {

        ReleaseSeatsResponse response = seatLockService.releaseSeats(scheduleId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seats released successfully", response));
    }

    @PostMapping("/cleanup-expired-locks")
    @Operation(summary = "Cleanup expired locks", description = "Manually triggers cleanup of expired temporary seat locks.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupExpiredLocks(@PathVariable UUID scheduleId) {
        int cleaned = seatLockService.cleanupExpiredLocks();
        return ResponseEntity.ok(ApiResponse.ok("Cleanup completed", Map.of("cleanedExpiredLocksCount", cleaned)));
    }
}
