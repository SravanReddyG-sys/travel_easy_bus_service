package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmationEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "Recipient email address", example = "traveller@gmail.com")
    private String toEmail;

    @Schema(description = "Passenger full name", example = "Sneha Singh")
    private String passengerName;

    @Schema(description = "Booking reference code", example = "TE-REF-4821")
    private String bookingReference;

    @Schema(description = "Bus number or identifier", example = "ROYAL-DELJAI-01")
    private String busNumber;

    @Schema(description = "Operator company name", example = "Royal Express Travels")
    private String operatorName;

    @Schema(description = "Origin city", example = "Bangalore")
    private String origin;

    @Schema(description = "Destination city", example = "Hyderabad")
    private String destination;

    @Schema(description = "Travel date", example = "2026-10-16")
    private String travelDate;

    @Schema(description = "Departure time", example = "05:30 AM")
    private String departureTime;

    @NotEmpty(message = "At least one seat must be booked")
    @Schema(description = "List of confirmed/held seat numbers", example = "[\"1A\", \"1B\"]")
    private List<String> seats;

    @Schema(description = "Total ticket amount in INR", example = "472.50")
    private BigDecimal totalAmount;

    @Schema(description = "Boarding point location", example = "Majestic Bus Station Platform 10")
    private String boardingPoint;

    @Schema(description = "Dropping point location", example = "Afzalgunj Hyderabad")
    private String droppingPoint;
}
