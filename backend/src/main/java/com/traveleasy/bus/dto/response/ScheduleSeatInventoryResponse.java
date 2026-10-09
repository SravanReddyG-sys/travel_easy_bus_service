package com.traveleasy.bus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.enums.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSeatInventoryResponse {

    @Schema(description = "Schedule seat inventory identifier")
    private UUID inventoryId;

    @Schema(description = "Physical seat identifier")
    private UUID seatId;

    @Schema(description = "Seat designation number", example = "1A")
    private String seatNumber;

    @Schema(description = "Deck level", example = "LOWER")
    private Deck deck;

    @Schema(description = "Grid row number", example = "1")
    private Integer rowNum;

    @Schema(description = "Grid column number", example = "1")
    private Integer colNum;

    @Schema(description = "Seat type", example = "SLEEPER")
    private SeatType seatType;

    @Schema(description = "Live booking/availability state", example = "AVAILABLE")
    private SeatInventoryStatus status;

    @Schema(description = "Whether seat is currently selectable and available for booking", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Seat ticket base price", example = "750.00")
    private BigDecimal basePrice;

    @Schema(description = "Seat ticket total price after tax and discounts", example = "787.50")
    private BigDecimal totalPrice;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when temporary lock expires if status is LOCKED")
    private LocalDateTime lockExpiryTime;
}
