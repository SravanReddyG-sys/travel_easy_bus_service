package com.traveleasy.bus.dto.request;

import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeatRequest {

    @NotBlank(message = "Seat number is required")
    @Size(min = 1, max = 10, message = "Seat number must be between 1 and 10 characters")
    @Schema(description = "Seat designation", example = "1A")
    private String seatNumber;

    @NotNull(message = "Deck is required")
    @Schema(description = "Deck level (LOWER or UPPER)", example = "LOWER")
    private Deck deck;

    @NotNull(message = "Row number is required")
    @Positive(message = "Row number must be positive")
    @Schema(description = "Grid row index", example = "1")
    private Integer rowNum;

    @NotNull(message = "Column number is required")
    @Positive(message = "Column number must be positive")
    @Schema(description = "Grid column index", example = "1")
    private Integer colNum;

    @NotNull(message = "Seat type is required")
    @Schema(description = "Seat physical type", example = "SLEEPER")
    private SeatType seatType;
}
