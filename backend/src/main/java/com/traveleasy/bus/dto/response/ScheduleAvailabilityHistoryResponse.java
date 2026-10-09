package com.traveleasy.bus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.traveleasy.bus.enums.AvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAvailabilityHistoryResponse {

    @Schema(description = "Availability audit record identifier")
    private UUID id;

    @Schema(description = "Schedule unique identifier")
    private UUID scheduleId;

    @Schema(description = "Availability status", example = "UNAVAILABLE")
    private AvailabilityStatus status;

    @Schema(description = "Reason for status change", example = "Road closure due to weather")
    private String reason;

    @Schema(description = "Principal or operator who modified the availability", example = "op@royal.com")
    private String modifiedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when change became effective")
    private LocalDateTime effectiveFrom;

    @Schema(description = "Record creation timestamp")
    private Instant createdAt;
}
