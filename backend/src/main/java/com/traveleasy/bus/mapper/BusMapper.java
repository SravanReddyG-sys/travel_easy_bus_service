package com.traveleasy.bus.mapper;

import com.traveleasy.bus.dto.request.CreateBusRequest;
import com.traveleasy.bus.dto.request.UpdateBusRequest;
import com.traveleasy.bus.dto.response.BusResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class BusMapper {

    public Bus toEntity(CreateBusRequest request, BusOperator operator) {
        if (request == null) {
            return null;
        }

        return Bus.builder()
                .operator(operator)
                .busNumber(request.getBusNumber().trim())
                .registrationNumber(request.getRegistrationNumber().trim().toUpperCase())
                .busType(request.getBusType())
                .totalSeats(request.getTotalSeats())
                .amenities(request.getAmenities() != null ? new ArrayList<>(request.getAmenities()) : new ArrayList<>())
                .build();
    }

    public void updateEntityFromDto(UpdateBusRequest request, Bus bus) {
        if (request == null || bus == null) {
            return;
        }

        bus.setBusNumber(request.getBusNumber().trim());
        bus.setBusType(request.getBusType());
        bus.setTotalSeats(request.getTotalSeats());
        if (request.getStatus() != null) {
            bus.setStatus(request.getStatus());
        }
        if (request.getAmenities() != null) {
            bus.setAmenities(new ArrayList<>(request.getAmenities()));
        }
    }

    public BusResponse toResponse(Bus bus) {
        if (bus == null) {
            return null;
        }

        return BusResponse.builder()
                .id(bus.getId())
                .operatorId(bus.getOperator() != null ? bus.getOperator().getId() : null)
                .operatorCompanyName(bus.getOperator() != null ? bus.getOperator().getCompanyName() : null)
                .operatorCode(bus.getOperator() != null ? bus.getOperator().getOperatorCode() : null)
                .busNumber(bus.getBusNumber())
                .registrationNumber(bus.getRegistrationNumber())
                .busType(bus.getBusType())
                .totalSeats(bus.getTotalSeats())
                .status(bus.getStatus())
                .amenities(bus.getAmenities() != null ? new ArrayList<>(bus.getAmenities()) : new ArrayList<>())
                .createdAt(bus.getCreatedAt())
                .updatedAt(bus.getUpdatedAt())
                .build();
    }
}
