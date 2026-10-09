package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusResponse {
    private UUID id;
    private UUID operatorId;
    private String operatorCompanyName;
    private String operatorCode;
    private String busNumber;
    private String registrationNumber;
    private BusType busType;
    private Integer totalSeats;
    private BusStatus status;
    private List<String> amenities;
    private Instant createdAt;
    private Instant updatedAt;
}
