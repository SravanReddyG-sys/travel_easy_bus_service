package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.CreateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusStatusRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.BusResponse;
import com.traveleasy.bus.dto.response.PageResponse;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.service.BusService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buses")
@RequiredArgsConstructor
@Tag(name = "Bus Management", description = "APIs for creating, querying, updating, and managing bus fleet assets")
public class BusController {

    private final BusService busService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a new bus", description = "Registers a new bus asset for the authenticated operator.")
    public ResponseEntity<ApiResponse<BusResponse>> createBus(@Valid @RequestBody CreateBusRequest request) {
        BusResponse response = busService.createBus(request);
        return new ResponseEntity<>(ApiResponse.ok("Bus registered successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bus by ID", description = "Retrieves complete bus details by unique ID.")
    public ResponseEntity<ApiResponse<BusResponse>> getBusById(@PathVariable UUID id) {
        BusResponse response = busService.getBusById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List buses", description = "Retrieves paginated buses. Operators see their own fleet; public/admins can filter.")
    public ResponseEntity<ApiResponse<PageResponse<BusResponse>>> listBuses(
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) BusStatus status,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true) Pageable pageable) {
        PageResponse<BusResponse> response = busService.listBuses(operatorId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update bus details", description = "Updates details of an existing bus owned by the operator.")
    public ResponseEntity<ApiResponse<BusResponse>> updateBus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBusRequest request) {
        BusResponse response = busService.updateBus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Bus updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update bus status", description = "Updates the operational status (ACTIVE, INACTIVE, MAINTENANCE).")
    public ResponseEntity<ApiResponse<BusResponse>> updateBusStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBusStatusRequest request) {
        BusResponse response = busService.updateBusStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Bus status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Deactivate/delete bus", description = "Soft-deletes or deactivates a bus fleet asset.")
    public ResponseEntity<ApiResponse<Void>> deleteBus(@PathVariable UUID id) {
        busService.deleteBus(id);
        return ResponseEntity.ok(ApiResponse.ok("Bus deactivated successfully", null));
    }
}
