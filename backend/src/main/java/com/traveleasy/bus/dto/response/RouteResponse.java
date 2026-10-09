package com.traveleasy.bus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private UUID id;
    private UUID operatorId;
    private String operatorCompanyName;
    private String operatorCode;
    private String routeCode;
    private String origin;
    private String destination;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private Boolean isActive;
    @Builder.Default
    private List<RouteStopResponse> stops = new ArrayList<>();
    @Builder.Default
    private List<BoardingPointResponse> boardingPoints = new ArrayList<>();
    @Builder.Default
    private List<DroppingPointResponse> droppingPoints = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
