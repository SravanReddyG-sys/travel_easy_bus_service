package com.traveleasy.bus.integration.booking;

import com.traveleasy.bus.integration.booking.dto.*;

import java.util.UUID;

public interface BusBookingIntegrationService {

    /**
     * Holds seats on a specific schedule for booking workflow.
     * Acquires DB pessimistic row-level lock and assigns a 10-minute TTL lockToken.
     */
    BookingSeatHoldResponse holdSeats(BookingSeatHoldRequest request);

    /**
     * Confirms previously locked seats upon successful payment and assigns booking reference.
     */
    BookingSeatConfirmResponse confirmSeats(BookingSeatConfirmRequest request);

    /**
     * Cancels confirmed booking seats and releases them back to AVAILABLE inventory status.
     */
    BookingSeatCancelResponse cancelBooking(BookingSeatCancelRequest request);

    /**
     * Checks current seat allocation status for a given booking reference.
     */
    BookingStatusResponse getBookingStatus(String bookingReference);
}
