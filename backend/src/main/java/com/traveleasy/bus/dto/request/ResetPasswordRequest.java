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
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "Registered account email address", example = "sneha.23mic7257@vitapstudent.ac.in")
    private String email;

    @NotBlank(message = "OTP reset code is required")
    @Schema(description = "6-digit OTP code sent via Resend email", example = "492817")
    private String otpCode;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    @Schema(description = "New account password", example = "NewPass@123")
    private String newPassword;
}
