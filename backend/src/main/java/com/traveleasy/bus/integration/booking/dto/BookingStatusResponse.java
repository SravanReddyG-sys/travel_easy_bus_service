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
public class BookingStatusResponse {

    private String bookingReference;
    private UUID scheduleId;
    private String busNumber;
    private String registrationNumber;
    private String origin;
    private String destination;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureTime;

    @Builder.Default
    private List<String> seatNumbers = new ArrayList<>();

    private String status;
}
