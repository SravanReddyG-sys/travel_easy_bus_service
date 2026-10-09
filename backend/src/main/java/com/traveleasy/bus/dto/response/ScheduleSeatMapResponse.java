package com.traveleasy.bus.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSeatMapResponse {

    @Schema(description = "Schedule unique identifier")
    private UUID scheduleId;

    @Schema(description = "Assigned bus identifier")
    private UUID busId;

    @Schema(description = "Bus fleet number", example = "BUS-001")
    private String busNumber;

    @Schema(description = "Bus vehicle registration number", example = "DL-01-AB-1234")
    private String registrationNumber;

    @Schema(description = "Total seats on the bus", example = "40")
    private Integer totalSeats;

    @Schema(description = "Count of currently available seats", example = "35")
    private Long availableSeatsCount;

    @Schema(description = "Seats located on the lower deck")
    @Builder.Default
    private List<ScheduleSeatInventoryResponse> lowerDeckSeats = new ArrayList<>();

    @Schema(description = "Seats located on the upper deck")
    @Builder.Default
    private List<ScheduleSeatInventoryResponse> upperDeckSeats = new ArrayList<>();
}
