package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopRequest {

    @NotBlank(message = "Stop name is required")
    @Size(min = 2, max = 100, message = "Stop name must be between 2 and 100 characters")
    @Schema(description = "Name of intermediate stop city or junction", example = "Gurgaon IFFCO Chowk")
    private String stopName;

    @NotNull(message = "Stop order is required")
    @Min(value = 1, message = "Stop order must be at least 1")
    @Schema(description = "Sequential order of the stop along the route (1, 2, 3...)", example = "1")
    private Integer stopOrder;

    @NotNull(message = "Distance from origin is required")
    @PositiveOrZero(message = "Distance from origin must be zero or positive")
    @Schema(description = "Distance in km from route origin", example = "30.0")
    private Double distanceFromOriginKm;
}
