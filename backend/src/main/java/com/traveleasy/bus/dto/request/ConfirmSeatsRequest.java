package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmSeatsRequest {

    @NotEmpty(message = "At least one seat number is required")
    @Schema(description = "List of seat numbers to confirm", example = "[\"1A\", \"1B\"]")
    private List<String> seatNumbers;

    @NotBlank(message = "Lock token is required for confirmation")
    @Schema(description = "Lock token provided during seat locking", example = "lock_9fa23e4c_...")
    private String lockToken;

    @NotBlank(message = "Booking reference is required")
    @Schema(description = "Unique central booking reference number", example = "BKG-20261201-9988")
    private String bookingReference;
}
