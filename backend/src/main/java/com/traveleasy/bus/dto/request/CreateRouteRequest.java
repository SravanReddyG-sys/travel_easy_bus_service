package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRouteRequest {

    @NotBlank(message = "Route code is required")
    @Size(min = 2, max = 50, message = "Route code must be between 2 and 50 characters")
    @Schema(description = "Unique operator route code", example = "RT-DEL-JAI-01")
    private String routeCode;

    @NotBlank(message = "Origin city/location is required")
    @Size(min = 2, max = 100, message = "Origin must be between 2 and 100 characters")
    @Schema(description = "Starting point of the route", example = "Delhi")
    private String origin;

    @NotBlank(message = "Destination city/location is required")
    @Size(min = 2, max = 100, message = "Destination must be between 2 and 100 characters")
    @Schema(description = "Ending point of the route", example = "Jaipur")
    private String destination;

    @NotNull(message = "Distance in kilometers is required")
    @Positive(message = "Distance must be greater than zero")
    @Schema(description = "Total route distance in kilometers", example = "280.0")
    private Double distanceKm;

    @NotNull(message = "Estimated duration in minutes is required")
    @Positive(message = "Estimated duration must be greater than zero")
    @Schema(description = "Estimated travel duration in minutes", example = "330")
    private Integer estimatedDurationMinutes;
}
