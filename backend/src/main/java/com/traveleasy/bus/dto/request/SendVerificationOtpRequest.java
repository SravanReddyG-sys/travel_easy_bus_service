package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendVerificationOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "User's email address", example = "sneha.23mic7257@vitapstudent.ac.in")
    private String email;

    @NotBlank(message = "Full name is required")
    @Schema(description = "Full name of the user", example = "Sneha Singh")
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Account password", example = "Pass@123")
    private String password;

    @Schema(description = "Role: TRAVELLER or OPERATOR", example = "TRAVELLER", defaultValue = "TRAVELLER")
    @Builder.Default
    private String role = "TRAVELLER";

    @Schema(description = "Bus operator company name (required if role is OPERATOR)", example = "Royal Express Travels")
    private String companyName;

    @Schema(description = "Contact phone number", example = "+91-9876543210")
    private String contactPhone;
}
