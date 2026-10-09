package com.traveleasy.bus.mapper;

import com.traveleasy.bus.dto.request.CreateFareRequest;
import com.traveleasy.bus.dto.request.UpdateFareRequest;
import com.traveleasy.bus.dto.response.FareResponse;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Schedule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FareMapper {

    public Fare toEntity(CreateFareRequest request, Schedule schedule) {
        if (request == null) {
            return null;
        }

        BigDecimal base = request.getBaseAmount();
        BigDecimal tax = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal total = base.add(tax).subtract(discount);

        String currency = (request.getCurrency() != null && !request.getCurrency().trim().isEmpty())
                ? request.getCurrency().trim().toUpperCase()
                : "INR";

        return Fare.builder()
                .schedule(schedule)
                .seatType(request.getSeatType())
                .baseAmount(base)
                .taxAmount(tax)
                .discountAmount(discount)
                .totalAmount(total)
                .currency(currency)
                .build();
    }

    public void updateEntityFromDto(UpdateFareRequest request, Fare fare) {
        if (request == null || fare == null) {
            return;
        }

        BigDecimal base = request.getBaseAmount();
        BigDecimal tax = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal total = base.add(tax).subtract(discount);

        fare.setBaseAmount(base);
        fare.setTaxAmount(tax);
        fare.setDiscountAmount(discount);
        fare.setTotalAmount(total);

        if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
            fare.setCurrency(request.getCurrency().trim().toUpperCase());
        }
    }

    public FareResponse toResponse(Fare fare) {
        if (fare == null) {
            return null;
        }

        return FareResponse.builder()
                .id(fare.getId())
                .scheduleId(fare.getSchedule() != null ? fare.getSchedule().getId() : null)
                .seatType(fare.getSeatType())
                .baseAmount(fare.getBaseAmount())
                .taxAmount(fare.getTaxAmount())
                .discountAmount(fare.getDiscountAmount())
                .totalAmount(fare.getTotalAmount())
                .currency(fare.getCurrency())
                .createdAt(fare.getCreatedAt())
                .updatedAt(fare.getUpdatedAt())
                .build();
    }
}
