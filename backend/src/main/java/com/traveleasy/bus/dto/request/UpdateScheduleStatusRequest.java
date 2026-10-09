package com.traveleasy.bus.dto.request;

import com.traveleasy.bus.enums.ScheduleStatus;
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
public class UpdateScheduleStatusRequest {

    @NotNull(message = "Schedule status is required")
    @Schema(description = "Target schedule lifecycle status", example = "BOARDING")
    private ScheduleStatus status;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    @Schema(description = "Optional reason or operational note", example = "Boarding commenced at Gate 4")
    private String reason;
}
