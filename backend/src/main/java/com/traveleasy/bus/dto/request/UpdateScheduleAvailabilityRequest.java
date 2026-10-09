package com.traveleasy.bus.dto.request;

import com.traveleasy.bus.enums.AvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduleAvailabilityRequest {

    @NotNull(message = "Availability status is required")
    @Schema(description = "Target availability status (AVAILABLE or UNAVAILABLE)", example = "UNAVAILABLE")
    private AvailabilityStatus availabilityStatus;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    @Schema(description = "Reason for changing availability status", example = "Bus undergoing unscheduled maintenance check")
    private String reason;
}
