package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusSearchResultCardResponse {

    private UUID scheduleId;
    private UUID busId;
    private String busNumber;
    private String registrationNumber;
    private BusType busType;
    
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    private UUID operatorId;
    private String operatorName;
    private String operatorCode;
    
    @Builder.Default
    private Double operatorRating = 4.5;

    private UUID routeId;
    private String routeCode;
    private String origin;
    private String destination;
    private Double distanceKm;

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer durationMinutes;

    private BigDecimal startingFare;
    
    @Builder.Default
    private String currency = "INR";

    private Long availableSeatsCount;
    private Integer totalSeatsCount;

    @Builder.Default
    private List<BoardingPointResponse> boardingPoints = new ArrayList<>();

    @Builder.Default
    private List<DroppingPointResponse> droppingPoints = new ArrayList<>();

    private ScheduleStatus scheduleStatus;
    private AvailabilityStatus availabilityStatus;
}
