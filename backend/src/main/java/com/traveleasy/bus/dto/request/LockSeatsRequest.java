package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockSeatsRequest {

    @NotEmpty(message = "At least one seat number is required to lock")
    @Schema(description = "List of seat numbers to lock", example = "[\"1A\", \"1B\"]")
    private List<String> seatNumbers;

    @Schema(description = "Optional user/booking session correlation ID", example = "sess_abc123")
    private String bookingSessionId;

    @Positive(message = "TTL in minutes must be positive")
    @Schema(description = "Lock duration in minutes (default 10)", example = "10", defaultValue = "10")
    @Builder.Default
    private Integer ttlMinutes = 10;
}
