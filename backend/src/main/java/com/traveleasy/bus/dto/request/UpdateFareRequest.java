package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFareRequest {

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.01", message = "Base amount must be greater than zero")
    @Schema(description = "Base ticket price", example = "850.00")
    private BigDecimal baseAmount;

    @DecimalMin(value = "0.00", message = "Tax amount cannot be negative")
    @Schema(description = "Applicable taxes/surcharges", example = "42.50")
    private BigDecimal taxAmount;

    @DecimalMin(value = "0.00", message = "Discount amount cannot be negative")
    @Schema(description = "Promotional or operator discount", example = "50.00")
    private BigDecimal discountAmount;

    @Size(min = 3, max = 10, message = "Currency must be between 3 and 10 characters")
    @Schema(description = "ISO Currency code", example = "INR")
    private String currency;
}
