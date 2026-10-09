package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.CreateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusStatusRequest;
import com.traveleasy.bus.dto.response.BusResponse;
import com.traveleasy.bus.dto.response.PageResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.exception.DuplicateResourceException;
import com.traveleasy.bus.exception.InvalidStateTransitionException;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.BusMapper;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.service.BusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final BusOperatorRepository operatorRepository;
    private final ScheduleRepository scheduleRepository;
    private final BusMapper busMapper;

    @Override
    @Transactional
    public BusResponse createBus(CreateBusRequest request) {
        UUID operatorId = SecurityUtils.requireCurrentOperatorId();
        log.info("Creating new bus for operator [{}] with registration [{}]", operatorId, request.getRegistrationNumber());

        // Ensure Operator entity exists or create default representation
        BusOperator operator = operatorRepository.findById(operatorId)
                .orElseGet(() -> {
                    UserPrincipal principal = SecurityUtils.requireCurrentUserPrincipal();
                    BusOperator newOperator = BusOperator.builder()
                            .operatorCode("OP-" + operatorId.toString().substring(0, 8).toUpperCase())
                            .companyName(principal.getEmail() != null ? principal.getEmail() : "Operator " + operatorId)
                            .contactEmail(principal.getEmail() != null ? principal.getEmail() : "operator@traveleasy.com")
                            .contactPhone("+910000000000")
                            .isActive(true)
                            .build();
                    newOperator.setId(operatorId);
                    return operatorRepository.save(newOperator);
                });

        // Validate uniqueness of registration number
        if (busRepository.existsByRegistrationNumber(request.getRegistrationNumber().trim().toUpperCase())) {
            throw new DuplicateResourceException("Bus", "registrationNumber", request.getRegistrationNumber());
        }

        // Validate uniqueness of busNumber for this operator
        if (busRepository.existsByOperatorIdAndBusNumber(operatorId, request.getBusNumber().trim())) {
            throw new DuplicateResourceException("Bus", "busNumber", request.getBusNumber());
        }

        Bus bus = busMapper.toEntity(request, operator);
        bus = busRepository.save(bus);
        log.info("Successfully created bus [{}] with id [{}]", bus.getBusNumber(), bus.getId());

        return busMapper.toResponse(bus);
    }

    @Override
    public BusResponse getBusById(UUID busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));

        // If authenticated user is an operator, check ownership
        Optional<UserPrincipal> principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent() && principalOpt.get().isOperator() && !principalOpt.get().isAdmin()) {
            SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());
        }

        return busMapper.toResponse(bus);
    }

    @Override
    public PageResponse<BusResponse> listBuses(UUID operatorIdFilter, BusStatus statusFilter, Pageable pageable) {
        Optional<UserPrincipal> principalOpt = SecurityUtils.getCurrentUserPrincipal();

        Page<Bus> busPage;
        if (principalOpt.isPresent() && principalOpt.get().isOperator() && !principalOpt.get().isAdmin()) {
            // Operator strictly sees only their own buses
            UUID authenticatedOperatorId = principalOpt.get().getOperatorId();
            busPage = busRepository.findByOperatorId(authenticatedOperatorId, pageable);
        } else if (operatorIdFilter != null) {
            // Admin or public filtering by specific operator
            busPage = busRepository.findByOperatorId(operatorIdFilter, pageable);
        } else {
            // Admin or public listing
            busPage = busRepository.findAll(pageable);
        }

        return PageResponse.from(busPage.map(busMapper::toResponse));
    }

    @Override
    @Transactional
    public BusResponse updateBus(UUID busId, UpdateBusRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));

        // Strict ownership check
        SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());

        // Check if busNumber changed and if new busNumber already exists for this operator
        if (!bus.getBusNumber().equalsIgnoreCase(request.getBusNumber().trim()) &&
                busRepository.existsByOperatorIdAndBusNumber(bus.getOperator().getId(), request.getBusNumber().trim())) {
            throw new DuplicateResourceException("Bus", "busNumber", request.getBusNumber());
        }

        busMapper.updateEntityFromDto(request, bus);
        bus = busRepository.save(bus);
        log.info("Updated bus [{}] with id [{}]", bus.getBusNumber(), bus.getId());

        return busMapper.toResponse(bus);
    }

    @Override
    @Transactional
    public BusResponse updateBusStatus(UUID busId, UpdateBusStatusRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));

        SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());

        bus.setStatus(request.getStatus());
        bus = busRepository.save(bus);
        log.info("Updated status of bus [{}] to [{}]", bus.getId(), request.getStatus());

        return busMapper.toResponse(bus);
    }

    @Override
    @Transactional
    public void deleteBus(UUID busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));

        SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());

        // Business Rule: Check if bus is scheduled for active/future trips
        var upcomingSchedules = scheduleRepository.findByBusIdAndScheduleDate(busId, LocalDate.now());
        boolean hasActiveSchedules = upcomingSchedules.stream()
                .anyMatch(s -> s.getStatus().name().equals("SCHEDULED") || s.getStatus().name().equals("BOARDING"));

        if (hasActiveSchedules) {
            throw new InvalidStateTransitionException("Bus", bus.getStatus().name(), "DELETED - Bus is currently assigned to active/scheduled journeys");
        }

        // Set to INACTIVE or delete
        bus.setStatus(BusStatus.INACTIVE);
        busRepository.save(bus);
        log.info("Deactivated/soft-deleted bus [{}]", busId);
    }
}
