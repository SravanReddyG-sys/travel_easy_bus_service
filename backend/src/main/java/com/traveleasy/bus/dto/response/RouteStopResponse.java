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
public class RouteStopResponse {
    private UUID id;
    private UUID routeId;
    private String stopName;
    private Integer stopOrder;
    private Double distanceFromOriginKm;
}
