package com.traveleasy.bus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    @Schema(description = "Schedule unique identifier")
    private UUID id;

    // Bus Details
    @Schema(description = "Assigned bus identifier")
    private UUID busId;

    @Schema(description = "Bus number in operator fleet", example = "BUS-001")
    private String busNumber;

    @Schema(description = "Bus registration number", example = "DL-01-AB-1234")
    private String busRegistrationNumber;

    @Schema(description = "Bus type", example = "AC_SLEEPER")
    private BusType busType;

    @Schema(description = "Total physical seats on bus", example = "40")
    private Integer totalSeats;

    // Route Details
    @Schema(description = "Assigned route identifier")
    private UUID routeId;

    @Schema(description = "Route code", example = "RT-DEL-JAI-01")
    private String routeCode;

    @Schema(description = "Origin location", example = "Delhi")
    private String origin;

    @Schema(description = "Destination location", example = "Jaipur")
    private String destination;

    @Schema(description = "Route distance in km", example = "280.0")
    private Double distanceKm;

    @Schema(description = "Estimated duration in minutes", example = "330")
    private Integer estimatedDurationMinutes;

    // Operator Details
    @Schema(description = "Bus operator identifier")
    private UUID operatorId;

    @Schema(description = "Bus operator company name", example = "Royal Express Travels")
    private String operatorCompanyName;

    // Schedule Times & Status
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of the journey", example = "2026-10-15")
    private LocalDate scheduleDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Departure timestamp", example = "2026-10-15T08:00:00")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Arrival timestamp", example = "2026-10-15T14:30:00")
    private LocalDateTime arrivalTime;

    @Schema(description = "Schedule lifecycle status", example = "SCHEDULED")
    private ScheduleStatus status;

    @Schema(description = "Schedule booking availability", example = "AVAILABLE")
    private AvailabilityStatus availabilityStatus;

    @Schema(description = "Cancellation reason if cancelled")
    private String cancellationReason;

    @Schema(description = "Count of currently available seats", example = "35")
    private Long availableSeatsCount;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
