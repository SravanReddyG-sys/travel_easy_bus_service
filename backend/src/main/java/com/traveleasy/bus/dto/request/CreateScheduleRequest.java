package com.traveleasy.bus.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduleRequest {

    @NotNull(message = "Bus ID is required")
    @Schema(description = "UUID of the assigned bus", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID busId;

    @NotNull(message = "Route ID is required")
    @Schema(description = "UUID of the assigned route", example = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID routeId;

    @NotNull(message = "Schedule date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of the schedule journey", example = "2026-10-15")
    private LocalDate scheduleDate;

    @NotNull(message = "Departure time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Departure timestamp", example = "2026-10-15T08:00:00")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Arrival timestamp", example = "2026-10-15T14:30:00")
    private LocalDateTime arrivalTime;
}
