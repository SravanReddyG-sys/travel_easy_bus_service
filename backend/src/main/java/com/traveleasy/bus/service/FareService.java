package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.CreateFareRequest;
import com.traveleasy.bus.dto.request.UpdateFareRequest;
import com.traveleasy.bus.dto.response.FareResponse;
import com.traveleasy.bus.enums.SeatType;

import java.util.List;
import java.util.UUID;

public interface FareService {

    FareResponse setFareForSeatType(UUID scheduleId, CreateFareRequest request);

    List<FareResponse> setFaresBulk(UUID scheduleId, List<CreateFareRequest> requests);

    List<FareResponse> getFaresByScheduleId(UUID scheduleId);

    FareResponse getFareByScheduleAndSeatType(UUID scheduleId, SeatType seatType);

    FareResponse updateFare(UUID scheduleId, UUID fareId, UpdateFareRequest request);

    void deleteFare(UUID scheduleId, UUID fareId);
}
