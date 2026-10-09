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
public class ReleaseSeatsResponse {

    @Schema(description = "Schedule unique identifier")
    private UUID scheduleId;

    @Schema(description = "List of released seat numbers now restored to AVAILABLE")
    private List<String> releasedSeats;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when seats were released")
    private LocalDateTime releasedAt;
}
