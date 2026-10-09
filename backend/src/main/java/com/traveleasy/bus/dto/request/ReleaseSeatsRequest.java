package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseSeatsRequest {

    @NotEmpty(message = "At least one seat number is required")
    @Schema(description = "List of seat numbers to release", example = "[\"1A\", \"1B\"]")
    private List<String> seatNumbers;

    @Schema(description = "Lock token if releasing temporarily locked seats", example = "lock_9fa23e4c_...")
    private String lockToken;

    @Schema(description = "Booking reference if releasing confirmed/booked seats upon cancellation", example = "BKG-20261201-9988")
    private String bookingReference;
}
