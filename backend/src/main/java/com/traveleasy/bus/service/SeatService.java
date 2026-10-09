package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.BlockSeatRequest;
import com.traveleasy.bus.dto.request.BulkSeatConfigRequest;
import com.traveleasy.bus.dto.request.CreateSeatRequest;
import com.traveleasy.bus.dto.response.PhysicalSeatResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatInventoryResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatMapResponse;

import java.util.List;
import java.util.UUID;

public interface SeatService {

    List<PhysicalSeatResponse> getBusSeats(UUID busId);

    List<PhysicalSeatResponse> configureBusSeats(UUID busId, BulkSeatConfigRequest request);

    PhysicalSeatResponse updateSeat(UUID busId, UUID seatId, CreateSeatRequest request);

    void deleteSeat(UUID busId, UUID seatId);

    ScheduleSeatMapResponse getScheduleSeatMap(UUID scheduleId);

    ScheduleSeatInventoryResponse blockOrUnblockSeat(UUID scheduleId, String seatNumber, BlockSeatRequest request);
}
