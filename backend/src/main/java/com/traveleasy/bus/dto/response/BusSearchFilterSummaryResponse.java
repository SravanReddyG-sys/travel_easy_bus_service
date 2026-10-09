package com.traveleasy.bus.dto.response;

import com.traveleasy.bus.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusSearchFilterSummaryResponse {

    @Builder.Default
    private Set<BusType> availableBusTypes = new HashSet<>();

    @Builder.Default
    private Set<String> availableOperators = new HashSet<>();

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    @Builder.Default
    private Set<String> availableBoardingPoints = new HashSet<>();

    @Builder.Default
    private Set<String> availableDroppingPoints = new HashSet<>();

    private long totalFound;
}
