package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareResponse {

    @Schema(description = "Fare unique identifier")
    private UUID id;

    @Schema(description = "Associated schedule identifier")
    private UUID scheduleId;

    @Schema(description = "Seat type for which this fare applies", example = "SLEEPER")
    private SeatType seatType;

    @Schema(description = "Base price before taxes and discounts", example = "750.00")
    private BigDecimal baseAmount;

    @Schema(description = "Taxes and fees", example = "37.50")
    private BigDecimal taxAmount;

    @Schema(description = "Discount applied", example = "0.00")
    private BigDecimal discountAmount;

    @Schema(description = "Final calculated price per seat (base + tax - discount)", example = "787.50")
    private BigDecimal totalAmount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
