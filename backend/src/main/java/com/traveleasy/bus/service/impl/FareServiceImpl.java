package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.CreateFareRequest;
import com.traveleasy.bus.dto.request.UpdateFareRequest;
import com.traveleasy.bus.dto.response.FareResponse;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.FareMapper;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.service.FareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FareServiceImpl implements FareService {

    private final FareRepository fareRepository;
    private final ScheduleRepository scheduleRepository;
    private final FareMapper fareMapper;

    @Override
    public FareResponse setFareForSeatType(UUID scheduleId, CreateFareRequest request) {
        log.info("Setting fare for schedule ID: {}, seatType: {}, baseAmount: {}",
                scheduleId, request.getSeatType(), request.getBaseAmount());

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());
        validateAmounts(request.getBaseAmount(), request.getTaxAmount(), request.getDiscountAmount());

        Optional<Fare> existingFareOpt = fareRepository.findByScheduleIdAndSeatType(scheduleId, request.getSeatType());
        Fare fareToSave;

        if (existingFareOpt.isPresent()) {
            fareToSave = existingFareOpt.get();
            UpdateFareRequest updateDto = UpdateFareRequest.builder()
                    .baseAmount(request.getBaseAmount())
                    .taxAmount(request.getTaxAmount())
                    .discountAmount(request.getDiscountAmount())
                    .currency(request.getCurrency())
                    .build();
            fareMapper.updateEntityFromDto(updateDto, fareToSave);
        } else {
            fareToSave = fareMapper.toEntity(request, schedule);
        }

        Fare savedFare = fareRepository.save(fareToSave);
        log.info("Successfully configured fare ID: {} for seat type: {}", savedFare.getId(), savedFare.getSeatType());
        return fareMapper.toResponse(savedFare);
    }

    @Override
    public List<FareResponse> setFaresBulk(UUID scheduleId, List<CreateFareRequest> requests) {
        log.info("Setting {} bulk fares for schedule ID: {}", requests != null ? requests.size() : 0, scheduleId);

        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        List<FareResponse> responses = new ArrayList<>();
        for (CreateFareRequest req : requests) {
            responses.add(setFareForSeatType(scheduleId, req));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FareResponse> getFaresByScheduleId(UUID scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found with ID: " + scheduleId);
        }

        return fareRepository.findByScheduleId(scheduleId).stream()
                .map(fareMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FareResponse getFareByScheduleAndSeatType(UUID scheduleId, SeatType seatType) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found with ID: " + scheduleId);
        }

        Fare fare = fareRepository.findByScheduleIdAndSeatType(scheduleId, seatType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fare not configured for schedule ID: " + scheduleId + " and seat type: " + seatType));

        return fareMapper.toResponse(fare);
    }

    @Override
    public FareResponse updateFare(UUID scheduleId, UUID fareId, UpdateFareRequest request) {
        log.info("Updating fare ID: {} on schedule ID: {}", fareId, scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());
        validateAmounts(request.getBaseAmount(), request.getTaxAmount(), request.getDiscountAmount());

        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found with ID: " + fareId));

        if (!fare.getSchedule().getId().equals(scheduleId)) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                    "Fare with ID " + fareId + " does not belong to schedule " + scheduleId);
        }

        fareMapper.updateEntityFromDto(request, fare);
        Fare updatedFare = fareRepository.save(fare);
        return fareMapper.toResponse(updatedFare);
    }

    @Override
    public void deleteFare(UUID scheduleId, UUID fareId) {
        log.info("Deleting fare ID: {} from schedule ID: {}", fareId, scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found with ID: " + fareId));

        if (!fare.getSchedule().getId().equals(scheduleId)) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                    "Fare with ID " + fareId + " does not belong to schedule " + scheduleId);
        }

        fareRepository.delete(fare);
    }

    private void validateAmounts(BigDecimal baseAmount, BigDecimal taxAmount, BigDecimal discountAmount) {
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;

        if (baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                    "Base amount must be greater than zero");
        }

        if (discount.compareTo(baseAmount.add(tax)) > 0) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                    "Discount amount cannot exceed total gross price (base + tax)");
        }
    }
}
