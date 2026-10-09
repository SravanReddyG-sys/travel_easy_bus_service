package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.UpdateScheduleAvailabilityRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleStatusRequest;
import com.traveleasy.bus.dto.response.ScheduleAvailabilityHistoryResponse;
import com.traveleasy.bus.dto.response.ScheduleResponse;
import com.traveleasy.bus.entity.BusAvailability;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.ScheduleMapper;
import com.traveleasy.bus.repository.BusAvailabilityRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.service.ScheduleAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleAvailabilityServiceImpl implements ScheduleAvailabilityService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatInventoryRepository inventoryRepository;
    private final BusAvailabilityRepository busAvailabilityRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    public ScheduleResponse updateScheduleAvailability(UUID scheduleId, UpdateScheduleAvailabilityRequest request) {
        log.info("Updating availability for schedule ID: {} to {}", scheduleId, request.getAvailabilityStatus());

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        schedule.setAvailabilityStatus(request.getAvailabilityStatus());
        Schedule savedSchedule = scheduleRepository.save(schedule);

        String modifier = SecurityUtils.getCurrentUserPrincipal()
                .map(UserPrincipal::getEmail)
                .orElse("OPERATOR");

        BusAvailability audit = BusAvailability.builder()
                .schedule(savedSchedule)
                .status(request.getAvailabilityStatus())
                .reason(request.getReason() != null ? request.getReason().trim() : null)
                .modifiedBy(modifier)
                .effectiveFrom(LocalDateTime.now())
                .build();
        busAvailabilityRepository.save(audit);

        long availableSeatsCount = inventoryRepository.countAvailableSeatsByScheduleId(scheduleId);
        return scheduleMapper.toResponse(savedSchedule, availableSeatsCount);
    }

    @Override
    public ScheduleResponse updateScheduleStatus(UUID scheduleId, UpdateScheduleStatusRequest request) {
        log.info("Updating lifecycle status for schedule ID: {} to {}", scheduleId, request.getStatus());

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        ScheduleStatus current = schedule.getStatus();
        ScheduleStatus target = request.getStatus();

        validateStatusTransition(current, target);

        schedule.setStatus(target);

        if (target == ScheduleStatus.CANCELLED) {
            schedule.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
            if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
                schedule.setCancellationReason(request.getReason().trim());
            }
        } else if (target == ScheduleStatus.COMPLETED) {
            schedule.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        }

        Schedule savedSchedule = scheduleRepository.save(schedule);
        long availableSeatsCount = inventoryRepository.countAvailableSeatsByScheduleId(scheduleId);
        return scheduleMapper.toResponse(savedSchedule, availableSeatsCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleAvailabilityHistoryResponse> getScheduleAvailabilityHistory(UUID scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found with ID: " + scheduleId);
        }

        return busAvailabilityRepository.findByScheduleIdOrderByEffectiveFromDesc(scheduleId).stream()
                .map(a -> ScheduleAvailabilityHistoryResponse.builder()
                        .id(a.getId())
                        .scheduleId(a.getSchedule().getId())
                        .status(a.getStatus())
                        .reason(a.getReason())
                        .modifiedBy(a.getModifiedBy())
                        .effectiveFrom(a.getEffectiveFrom())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateStatusTransition(ScheduleStatus current, ScheduleStatus target) {
        if (current == target) {
            return; // Idempotent
        }

        if (current == ScheduleStatus.COMPLETED) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATE_TRANSITION,
                    "Schedule is COMPLETED and cannot transition to " + target);
        }

        if (current == ScheduleStatus.CANCELLED) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATE_TRANSITION,
                    "Schedule is CANCELLED and cannot transition to " + target);
        }

        boolean valid = switch (current) {
            case SCHEDULED -> (target == ScheduleStatus.BOARDING || target == ScheduleStatus.CANCELLED);
            case BOARDING -> (target == ScheduleStatus.DEPARTED || target == ScheduleStatus.CANCELLED);
            case DEPARTED -> (target == ScheduleStatus.COMPLETED || target == ScheduleStatus.CANCELLED);
            default -> false;
        };

        if (!valid) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATE_TRANSITION,
                    "Invalid state transition from " + current + " to " + target);
        }
    }
}
