package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.SeatInventoryStatus;
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
public class OperatorLiveBookingFeedResponse {
    private UUID scheduleId;
    private String busNumber;
    private String routeSummary;
    private String seatNumber;
    private SeatInventoryStatus status;
    private String bookingReference;
    private BigDecimal amount;
    private LocalDateTime eventTimestamp;
}
