package com.traveleasy.bus.integration.booking;

import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.integration.booking.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integration/booking")
@RequiredArgsConstructor
@Tag(name = "Booking Service Integration", description = "Dedicated inter-service contracts and adapter APIs for Booking Management Service")
public class BusBookingIntegrationController {

    private final BusBookingIntegrationService bookingIntegrationService;

    @PostMapping("/hold")
    @Operation(summary = "Hold seats for checkout",
               description = "Inter-service endpoint: locks seats for 10 minutes with pessimistic DB write locks and returns hold token & price breakdown.")
    public ResponseEntity<ApiResponse<BookingSeatHoldResponse>> holdSeats(
            @Valid @RequestBody BookingSeatHoldRequest request) {
        BookingSeatHoldResponse response = bookingIntegrationService.holdSeats(request);
        return new ResponseEntity<>(ApiResponse.ok("Seats placed on hold successfully", response), HttpStatus.OK);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm booking seats",
               description = "Inter-service endpoint: transitions seats from LOCKED to BOOKED upon successful payment confirmation.")
    public ResponseEntity<ApiResponse<BookingSeatConfirmResponse>> confirmSeats(
            @Valid @RequestBody BookingSeatConfirmRequest request) {
        BookingSeatConfirmResponse response = bookingIntegrationService.confirmSeats(request);
        return ResponseEntity.ok(ApiResponse.ok("Seats confirmed and booked successfully", response));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel booking seats",
               description = "Inter-service endpoint: cancels booked seats and restores inventory back to AVAILABLE.")
    public ResponseEntity<ApiResponse<BookingSeatCancelResponse>> cancelBooking(
            @Valid @RequestBody BookingSeatCancelRequest request) {
        BookingSeatCancelResponse response = bookingIntegrationService.cancelBooking(request);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled and seats released", response));
    }

    @GetMapping("/status/{bookingReference}")
    @Operation(summary = "Get booking seat status",
               description = "Inter-service endpoint: queries the real-time status of seats allocated to a booking reference.")
    public ResponseEntity<ApiResponse<BookingStatusResponse>> getBookingStatus(
            @PathVariable String bookingReference) {
        BookingStatusResponse response = bookingIntegrationService.getBookingStatus(bookingReference);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
