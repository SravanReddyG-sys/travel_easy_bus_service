package com.traveleasy.bus.dto.request;

import com.traveleasy.bus.enums.BusType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBusRequest {

    @NotBlank(message = "Bus number is required")
    @Size(min = 2, max = 50, message = "Bus number must be between 2 and 50 characters")
    @Schema(description = "Operator fleet bus number", example = "BUS-VOLVO-101")
    private String busNumber;

    @NotBlank(message = "Registration number is required")
    @Size(min = 4, max = 50, message = "Registration number must be between 4 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9 -]+$", message = "Registration number must contain alphanumeric characters")
    @Schema(description = "Official government vehicle registration / license plate", example = "DL-01-AB-1234")
    private String registrationNumber;

    @NotNull(message = "Bus type is required")
    @Schema(description = "Type of bus", example = "VOLVO_MULTI_AXLE")
    private BusType busType;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Bus must have at least 1 seat")
    @Max(value = 100, message = "Bus cannot have more than 100 seats")
    @Schema(description = "Total seating capacity", example = "40")
    private Integer totalSeats;

    @Builder.Default
    @Schema(description = "List of amenities provided on the bus", example = "[\"WiFi\", \"AC\", \"USB Charging\"]")
    private List<String> amenities = new ArrayList<>();
}
