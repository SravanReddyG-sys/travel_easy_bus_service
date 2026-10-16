package com.traveleasy.bus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "User's email address", example = "sneha.23mic7257@vitapstudent.ac.in")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Schema(description = "6-digit OTP code sent via Resend email", example = "492817")
    private String otpCode;
}
