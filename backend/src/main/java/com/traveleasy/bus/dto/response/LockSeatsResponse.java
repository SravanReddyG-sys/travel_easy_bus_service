package com.traveleasy.bus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockSeatsResponse {

    @Schema(description = "Schedule unique identifier")
    private UUID scheduleId;

    @Schema(description = "List of successfully locked seat numbers")
    private List<String> lockedSeats;

    @Schema(description = "Unique lock correlation token required for confirmation or early release")
    private String lockToken;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when the lock will expire if not confirmed")
    private LocalDateTime lockExpiresAt;

    @Schema(description = "Total price calculated for all locked seats", example = "1575.00")
    private BigDecimal totalAmount;

    @Schema(description = "Currency", example = "INR")
    private String currency;
}
