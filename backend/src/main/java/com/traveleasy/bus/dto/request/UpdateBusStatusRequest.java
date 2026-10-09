package com.traveleasy.bus.dto.request;

import com.traveleasy.bus.enums.BusStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBusStatusRequest {

    @NotNull(message = "Bus status is required")
    @Schema(description = "New operational status for the bus", example = "MAINTENANCE")
    private BusStatus status;
}
