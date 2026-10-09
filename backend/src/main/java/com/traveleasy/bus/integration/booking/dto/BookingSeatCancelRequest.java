package com.traveleasy.bus.integration.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatCancelRequest {

    @NotNull(message = "Schedule ID is required")
    private UUID scheduleId;

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    private List<String> seatNumbers;

    private String cancellationReason;
}
