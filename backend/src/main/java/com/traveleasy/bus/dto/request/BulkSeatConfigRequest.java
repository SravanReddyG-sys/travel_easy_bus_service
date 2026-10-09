package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
public class BulkSeatConfigRequest {

    @NotEmpty(message = "At least one seat configuration is required")
    @Valid
    @Schema(description = "List of physical seats to configure on the bus")
    private List<CreateSeatRequest> seats;
}
