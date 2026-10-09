package com.traveleasy.bus.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorRevenueStatResponse {

    @Schema(description = "Route unique identifier")
    private UUID routeId;

    @Schema(description = "Route code", example = "RT-DEL-JAI")
    private String routeCode;

    @Schema(description = "Route origin", example = "Delhi")
    private String origin;

    @Schema(description = "Route destination", example = "Jaipur")
    private String destination;

    @Schema(description = "Total number of schedules operated on this route")
    private long schedulesCount;

    @Schema(description = "Total seats booked on this route")
    private long totalBookedSeats;

    @Schema(description = "Total revenue earned from this route", example = "45000.00")
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Schema(description = "Currency", example = "INR")
    @Builder.Default
    private String currency = "INR";
}
