package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.CreateScheduleRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleRequest;
import com.traveleasy.bus.dto.response.PageResponse;
import com.traveleasy.bus.dto.response.ScheduleResponse;
import com.traveleasy.bus.dto.response.TravellerScheduleDetailResponse;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface ScheduleService {

    ScheduleResponse createSchedule(CreateScheduleRequest request);

    ScheduleResponse getScheduleById(UUID scheduleId);

    PageResponse<ScheduleResponse> getSchedules(
            UUID operatorId,
            UUID routeId,
            UUID busId,
            LocalDate scheduleDate,
            ScheduleStatus status,
            AvailabilityStatus availabilityStatus,
            Pageable pageable);

    PageResponse<ScheduleResponse> getOperatorSchedules(Pageable pageable);

    ScheduleResponse updateSchedule(UUID scheduleId, UpdateScheduleRequest request);

    ScheduleResponse cancelSchedule(UUID scheduleId, String cancellationReason);

    void deleteSchedule(UUID scheduleId);

    TravellerScheduleDetailResponse getTravellerScheduleDetails(UUID scheduleId);
}
