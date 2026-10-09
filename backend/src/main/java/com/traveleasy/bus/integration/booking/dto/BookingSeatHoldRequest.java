package com.traveleasy.bus.integration.booking.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class BookingSeatHoldRequest {

    @NotNull(message = "Schedule ID is required")
    private UUID scheduleId;

    @NotEmpty(message = "At least one seat number must be specified")
    private List<String> seatNumbers;

    private UUID userId;

    @Builder.Default
    private int holdDurationMinutes = 10;
}
