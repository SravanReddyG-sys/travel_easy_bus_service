package com.traveleasy.bus.dto.request;

import com.traveleasy.bus.enums.BusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusSearchRequest {

    @NotBlank(message = "Origin city is required")
    private String origin;

    @NotBlank(message = "Destination city is required")
    private String destination;

    @NotNull(message = "Travel date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate travelDate;

    private BusType busType;

    private UUID operatorId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private List<String> amenities;

    private String boardingPoint;

    private String droppingPoint;

    /**
     * Sort option: price_asc, price_desc, departure_asc, departure_desc, duration_asc
     */
    @Builder.Default
    private String sortBy = "departure_asc";

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
