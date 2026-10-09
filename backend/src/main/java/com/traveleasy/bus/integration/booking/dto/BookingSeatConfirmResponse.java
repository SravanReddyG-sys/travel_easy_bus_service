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
public class BookingSeatConfirmResponse {

    private boolean confirmed;
    private String bookingReference;
    private UUID scheduleId;

    @Builder.Default
    private List<String> confirmedSeats = new ArrayList<>();

    private BigDecimal totalAmount;
    
    @Builder.Default
    private String currency = "INR";

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureTime;

    private String busRegistrationNumber;
    private String origin;
    private String destination;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime confirmedAt;
}
