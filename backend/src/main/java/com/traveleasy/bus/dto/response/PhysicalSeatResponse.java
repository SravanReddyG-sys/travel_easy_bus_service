package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalSeatResponse {

    @Schema(description = "Seat unique identifier")
    private UUID id;

    @Schema(description = "Assigned bus identifier")
    private UUID busId;

    @Schema(description = "Seat designation number", example = "1A")
    private String seatNumber;

    @Schema(description = "Deck location", example = "LOWER")
    private Deck deck;

    @Schema(description = "Row position in grid", example = "1")
    private Integer rowNum;

    @Schema(description = "Column position in grid", example = "1")
    private Integer colNum;

    @Schema(description = "Seat type", example = "SLEEPER")
    private SeatType seatType;

    @Schema(description = "Whether seat is active and installable")
    private Boolean isActive;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
