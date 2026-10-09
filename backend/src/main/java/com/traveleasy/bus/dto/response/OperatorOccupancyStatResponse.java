package com.traveleasy.bus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class OperatorOccupancyStatResponse {

    @Schema(description = "Schedule identifier")
    private UUID scheduleId;

    @Schema(description = "Bus number", example = "BUS-101")
    private String busNumber;

    @Schema(description = "Route origin", example = "Delhi")
    private String origin;

    @Schema(description = "Route destination", example = "Jaipur")
    private String destination;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Schedule date")
    private LocalDate scheduleDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Departure timestamp")
    private LocalDateTime departureTime;

    @Schema(description = "Total capacity seats")
    private int totalSeats;

    @Schema(description = "Booked seats count")
    private long bookedSeats;

    @Schema(description = "Available seats count")
    private long availableSeats;

    @Schema(description = "Occupancy percentage for this trip", example = "85.0")
    private Double occupancyPercentage;
}
