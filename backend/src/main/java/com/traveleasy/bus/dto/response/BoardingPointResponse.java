package com.traveleasy.bus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardingPointResponse {
    private UUID id;
    private UUID routeId;
    private UUID stopId;
    private String stopName;
    private String locationName;
    private String landmark;
    private Integer departureOffsetMinutes;
    private String contactPhone;
}
