package com.traveleasy.bus.integration.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatConfirmRequest {

    @NotNull(message = "Schedule ID is required")
    private UUID scheduleId;

    @NotBlank(message = "Booking reference code is required")
    private String bookingReference;

    @NotBlank(message = "Lock token is required")
    private String lockToken;

    @NotEmpty(message = "At least one seat number is required")
    private List<String> seatNumbers;

    @Builder.Default
    private List<PassengerSeatDetail> passengers = new ArrayList<>();
}
