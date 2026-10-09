package com.traveleasy.bus.integration.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerSeatDetail {

    private String seatNumber;
    private String passengerName;
    private Integer passengerAge;
    private String passengerGender;
}
