package com.traveleasy.bus.integration.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatCancelResponse {

    private boolean cancelled;
    private String bookingReference;
    private UUID scheduleId;

    @Builder.Default
    private List<String> releasedSeats = new ArrayList<>();

    private boolean refundEligible;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;
}
