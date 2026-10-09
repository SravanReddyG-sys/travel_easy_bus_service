package com.traveleasy.bus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmSeatsResponse {

    @Schema(description = "Schedule unique identifier")
    private UUID scheduleId;

    @Schema(description = "List of confirmed booked seat numbers")
    private List<String> bookedSeats;

    @Schema(description = "Associated central booking reference")
    private String bookingReference;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Booking confirmation timestamp")
    private LocalDateTime confirmedAt;
}
