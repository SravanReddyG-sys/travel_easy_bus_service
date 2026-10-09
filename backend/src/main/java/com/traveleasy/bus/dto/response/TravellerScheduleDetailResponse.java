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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravellerScheduleDetailResponse {

    @Schema(description = "Schedule unique identifier")
    private UUID scheduleId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of the journey", example = "2026-10-15")
    private LocalDate scheduleDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Departure timestamp", example = "2026-10-15T08:00:00")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Arrival timestamp", example = "2026-10-15T14:30:00")
    private LocalDateTime arrivalTime;

    @Schema(description = "Estimated journey duration in minutes", example = "390")
    private Integer durationMinutes;

    @Schema(description = "Schedule status", example = "SCHEDULED")
    private ScheduleStatus status;

    @Schema(description = "Booking availability status", example = "AVAILABLE")
    private AvailabilityStatus availabilityStatus;

    @Schema(description = "Cancellation reason if schedule was cancelled")
    private String cancellationReason;

    // Bus Fleet Details
    @Schema(description = "Bus unique identifier")
    private UUID busId;

    @Schema(description = "Bus fleet number", example = "BUS-101")
    private String busNumber;

    @Schema(description = "Vehicle registration number", example = "DL-01-AB-1234")
    private String registrationNumber;

    @Schema(description = "Bus type", example = "AC_SLEEPER")
    private BusType busType;

    @Schema(description = "Total physical seats", example = "40")
    private Integer totalSeats;

    @Schema(description = "Bus onboard amenities")
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    // Operator Information
    @Schema(description = "Bus operator identifier")
    private UUID operatorId;

    @Schema(description = "Bus operator company name", example = "Royal Express Travels")
    private String operatorCompanyName;

    @Schema(description = "Bus operator unique code", example = "OP-ROYAL")
    private String operatorCode;

    @Schema(description = "Operator contact phone")
    private String contactPhone;

    @Schema(description = "Operator contact email")
    private String contactEmail;

    @Schema(description = "Operator aggregate rating", example = "4.5")
    @Builder.Default
    private Double operatorRating = 4.5;

    // Route Information
    @Schema(description = "Route unique identifier")
    private UUID routeId;

    @Schema(description = "Route code", example = "RT-DEL-JAI")
    private String routeCode;

    @Schema(description = "Origin location", example = "Delhi")
    private String origin;

    @Schema(description = "Destination location", example = "Jaipur")
    private String destination;

    @Schema(description = "Total route distance in km", example = "280.0")
    private Double distanceKm;

    @Schema(description = "Route intermediate stops")
    @Builder.Default
    private List<RouteStopResponse> stops = new ArrayList<>();

    @Schema(description = "Boarding points with departure offsets")
    @Builder.Default
    private List<BoardingPointResponse> boardingPoints = new ArrayList<>();

    @Schema(description = "Dropping points with arrival offsets")
    @Builder.Default
    private List<DroppingPointResponse> droppingPoints = new ArrayList<>();

    // Fares Structure
    @Schema(description = "Seat pricing structure by seat type")
    @Builder.Default
    private List<FareResponse> fares = new ArrayList<>();

    // Live Seat Map & Inventory
    @Schema(description = "Complete seat layout with real-time seat lock and booking availability")
    private ScheduleSeatMapResponse seatMap;

    // Policies
    @Schema(description = "Cancellation policy terms")
    @Builder.Default
    private String cancellationPolicy = "Free cancellation up to 6 hours before departure. 50% refund between 2 to 6 hours before departure. Non-refundable within 2 hours of departure.";

    @Schema(description = "General terms and conditions")
    @Builder.Default
    private String termsAndConditions = "Passengers must carry a valid government-issued photo ID. Luggage up to 15kg per passenger is complimentary.";
}
