package com.traveleasy.bus.integration.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatHoldResponse {

    private String lockToken;
    private UUID scheduleId;
    
    @Builder.Default
    private List<String> lockedSeats = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lockExpiryTime;

    @Builder.Default
    private List<SeatPriceBreakdown> seatPrices = new ArrayList<>();

    @Builder.Default
    private BigDecimal totalHoldAmount = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "INR";
}
