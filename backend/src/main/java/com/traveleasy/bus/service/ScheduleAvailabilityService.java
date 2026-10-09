package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.UpdateScheduleAvailabilityRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleStatusRequest;
import com.traveleasy.bus.dto.response.ScheduleAvailabilityHistoryResponse;
import com.traveleasy.bus.dto.response.ScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduleAvailabilityService {

    ScheduleResponse updateScheduleAvailability(UUID scheduleId, UpdateScheduleAvailabilityRequest request);

    ScheduleResponse updateScheduleStatus(UUID scheduleId, UpdateScheduleStatusRequest request);

    List<ScheduleAvailabilityHistoryResponse> getScheduleAvailabilityHistory(UUID scheduleId);
}
