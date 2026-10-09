package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.BlockSeatRequest;
import com.traveleasy.bus.dto.request.BulkSeatConfigRequest;
import com.traveleasy.bus.dto.request.CreateSeatRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.PhysicalSeatResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatInventoryResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatMapResponse;
import com.traveleasy.bus.service.SeatService;
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
@RequiredArgsConstructor
@Tag(name = "Seat Layout & Inventory", description = "APIs for configuring physical bus seating layouts and querying schedule seat maps")
public class SeatController {

    private final SeatService seatService;

    // ================= Bus Physical Seat Endpoints =================

    @GetMapping("/api/v1/buses/{busId}/seats")
    @Operation(summary = "Get bus physical seats", description = "Retrieves all configured physical seats and layout for a bus.")
    public ResponseEntity<ApiResponse<List<PhysicalSeatResponse>>> getBusSeats(@PathVariable UUID busId) {
        List<PhysicalSeatResponse> response = seatService.getBusSeats(busId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/v1/buses/{busId}/seats")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Configure bus seats", description = "Bulk configures or updates the physical seat grid on a bus.")
    public ResponseEntity<ApiResponse<List<PhysicalSeatResponse>>> configureBusSeats(
            @PathVariable UUID busId,
            @Valid @RequestBody BulkSeatConfigRequest request) {

        List<PhysicalSeatResponse> response = seatService.configureBusSeats(busId, request);
        return new ResponseEntity<>(ApiResponse.ok("Bus seat layout configured successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/api/v1/buses/{busId}/seats/{seatId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update bus seat", description = "Updates a physical seat's configuration.")
    public ResponseEntity<ApiResponse<PhysicalSeatResponse>> updateSeat(
            @PathVariable UUID busId,
            @PathVariable UUID seatId,
            @Valid @RequestBody CreateSeatRequest request) {

        PhysicalSeatResponse response = seatService.updateSeat(busId, seatId, request);
        return ResponseEntity.ok(ApiResponse.ok("Seat updated successfully", response));
    }

    @DeleteMapping("/api/v1/buses/{busId}/seats/{seatId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete bus seat", description = "Deletes a physical seat from a bus.")
    public ResponseEntity<ApiResponse<Void>> deleteSeat(
            @PathVariable UUID busId,
            @PathVariable UUID seatId) {

        seatService.deleteSeat(busId, seatId);
        return ResponseEntity.ok(ApiResponse.ok("Seat deleted successfully", null));
    }

    // ================= Schedule Dynamic Seat Map Endpoints =================

    @GetMapping("/api/v1/schedules/{scheduleId}/seats")
    @Operation(summary = "Get schedule seat map", description = "Retrieves complete 2D upper/lower deck seat map with live availability and pricing.")
    public ResponseEntity<ApiResponse<ScheduleSeatMapResponse>> getScheduleSeatMap(@PathVariable UUID scheduleId) {
        ScheduleSeatMapResponse response = seatService.getScheduleSeatMap(scheduleId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/api/v1/schedules/{scheduleId}/seats/{seatNumber}/block")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Block or unblock seat", description = "Allows operator to block a seat for maintenance/crew or restore it to AVAILABLE.")
    public ResponseEntity<ApiResponse<ScheduleSeatInventoryResponse>> blockOrUnblockSeat(
            @PathVariable UUID scheduleId,
            @PathVariable String seatNumber,
            @Valid @RequestBody BlockSeatRequest request) {

        ScheduleSeatInventoryResponse response = seatService.blockOrUnblockSeat(scheduleId, seatNumber, request);
        String action = Boolean.TRUE.equals(request.getBlocked()) ? "blocked" : "unblocked";
        return ResponseEntity.ok(ApiResponse.ok("Seat " + seatNumber + " successfully " + action, response));
    }
}
