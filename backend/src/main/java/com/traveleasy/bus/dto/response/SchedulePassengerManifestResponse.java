package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePassengerManifestResponse {
    private UUID scheduleId;
    private LocalDate scheduleDate;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private ScheduleStatus status;
    private AvailabilityStatus availabilityStatus;

    // Bus Details
    private UUID busId;
    private String busNumber;
    private String registrationNumber;
    private BusType busType;

    // Route Details
    private UUID routeId;
    private String routeCode;
    private String origin;
    private String destination;

    // Inventory & Revenue Stats
    private int totalSeats;
    private int bookedSeatsCount;
    private int lockedSeatsCount;
    private int availableSeatsCount;
    private int blockedSeatsCount;
    private double occupancyPercentage;
    private BigDecimal grossRevenue;
    private String currency;

    // Live Seat Roster
    private List<SeatReservationItemResponse> seatRoster;
}
