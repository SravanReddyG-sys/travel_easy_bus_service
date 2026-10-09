package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockSeatRequest {

    @NotNull(message = "Block status flag is required")
    @Schema(description = "True to block the seat, false to unblock (restore to AVAILABLE)", example = "true")
    private Boolean blocked;

    @Schema(description = "Optional reason for blocking seat (e.g. crew seat, VIP reservation, physical defect)", example = "Driver / Crew Reserved")
    private String reason;
}
