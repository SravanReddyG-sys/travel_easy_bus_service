package com.traveleasy.bus.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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
public class UpdateScheduleRequest {

    @Schema(description = "UUID of the assigned bus", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID busId;

    @Schema(description = "UUID of the assigned route", example = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID routeId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of the schedule journey", example = "2026-10-15")
    private LocalDate scheduleDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Departure timestamp", example = "2026-10-15T08:00:00")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Arrival timestamp", example = "2026-10-15T14:30:00")
    private LocalDateTime arrivalTime;

    @Schema(description = "Schedule lifecycle status", example = "SCHEDULED")
    private ScheduleStatus status;

    @Schema(description = "Schedule availability status", example = "AVAILABLE")
    private AvailabilityStatus availabilityStatus;

    @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
    @Schema(description = "Reason for schedule cancellation if cancelled", example = "Technical breakdown")
    private String cancellationReason;
}
