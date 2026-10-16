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
public class SendEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient must be a valid email address")
    @Schema(description = "Recipient email address", example = "traveller@gmail.com")
    private String to;

    @NotBlank(message = "Email subject is required")
    @Schema(description = "Email subject", example = "Welcome to TravelEasy!")
    private String subject;

    @Schema(description = "HTML content of the email")
    private String htmlContent;

    @Schema(description = "Plain text content (optional fallback)")
    private String text;
}
