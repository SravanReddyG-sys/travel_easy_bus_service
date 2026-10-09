package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.CreateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusStatusRequest;
import com.traveleasy.bus.dto.response.BusResponse;
import com.traveleasy.bus.dto.response.PageResponse;
import com.traveleasy.bus.enums.BusStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BusService {
    BusResponse createBus(CreateBusRequest request);
    BusResponse getBusById(UUID busId);
    PageResponse<BusResponse> listBuses(UUID operatorIdFilter, BusStatus statusFilter, Pageable pageable);
    BusResponse updateBus(UUID busId, UpdateBusRequest request);
    BusResponse updateBusStatus(UUID busId, UpdateBusStatusRequest request);
    void deleteBus(UUID busId);
}
