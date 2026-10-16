package com.traveleasy.bus.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendResponse {

    @Schema(description = "Resend Email ID or unique reference", example = "49633868-51f4-4320-9cb6-33640bfced1a")
    private String id;

    @Schema(description = "Delivery status: SENT, SIMULATED, or FAILED", example = "SENT")
    private String status;

    @Schema(description = "Recipient email address", example = "traveller@gmail.com")
    private String recipient;

    @Schema(description = "Email subject", example = "Welcome to TravelEasy!")
    private String subject;

    @Schema(description = "Informative status message", example = "Email delivered successfully via Resend")
    private String message;

    @Schema(description = "Whether Resend API key is configured on server", example = "true")
    private boolean resendConfigured;
}
