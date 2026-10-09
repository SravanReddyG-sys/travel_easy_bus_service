package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroppingPointRequest {

    @Schema(description = "Optional associated RouteStop ID", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID stopId;

    @NotBlank(message = "Location name is required")
    @Size(min = 2, max = 150, message = "Location name must be between 2 and 150 characters")
    @Schema(description = "Name of the passenger dropping location", example = "Sindhi Camp Bus Stand")
    private String locationName;

    @Size(max = 200, message = "Landmark cannot exceed 200 characters")
    @Schema(description = "Nearby landmark or platform", example = "Platform 4")
    private String landmark;

    @NotNull(message = "Arrival offset minutes is required")
    @Min(value = 0, message = "Arrival offset minutes cannot be negative")
    @Schema(description = "Arrival offset from schedule arrival time in minutes", example = "330")
    private Integer arrivalOffsetMinutes;

    @Size(max = 25, message = "Contact phone cannot exceed 25 characters")
    @Schema(description = "Dropping point contact/helpdesk phone", example = "+919876543210")
    private String contactPhone;
}
