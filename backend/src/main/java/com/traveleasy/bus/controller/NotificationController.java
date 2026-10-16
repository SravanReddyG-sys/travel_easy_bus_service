package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.BookingConfirmationEmailRequest;
import com.traveleasy.bus.dto.request.SendEmailRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.EmailSendResponse;
import com.traveleasy.bus.service.email.ResendEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/email")
@RequiredArgsConstructor
@Tag(name = "Resend Email Notifications", description = "Transactional email notifications powered by Resend API (Welcome emails, Booking confirmations, Test delivery)")
public class NotificationController {

    private final ResendEmailService resendEmailService;

    @GetMapping("/status")
    @Operation(summary = "Check Resend email service status", description = "Returns whether Resend API key is configured and active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        boolean configured = resendEmailService.isConfigured();
        String sender = resendEmailService.getSenderEmail();
        Map<String, Object> data = Map.of(
                "provider", "Resend",
                "configured", configured,
                "senderEmail", sender,
                "supportedTypes", new String[]{"WELCOME", "BOOKING_CONFIRMATION", "CUSTOM"}
        );
        return ResponseEntity.ok(ApiResponse.ok("Resend status retrieved", data));
    }

    @PostMapping("/test")
    @Operation(summary = "Send test email via Resend", description = "Sends a test email to the specified recipient to verify Resend API connectivity")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendTestEmail(@Valid @RequestBody SendEmailRequest request) {
        log.info("Sending test email to [{}] with subject '{}'", request.getTo(), request.getSubject());
        String html = (request.getHtmlContent() != null && !request.getHtmlContent().isBlank())
                ? request.getHtmlContent()
                : "<div style='font-family: sans-serif; padding: 20px; background: #f8fafc; border-radius: 8px;'>"
                + "<h2 style='color: #1e3c72;'>🚌 TravelEasy Email Verification</h2>"
                + "<p>This is a live test notification delivered via <strong>Resend</strong> for TravelEasy Bus Platform.</p>"
                + "<p style='color: #64748b; font-size: 13px;'>If you received this message, Resend email integration is working perfectly!</p>"
                + "</div>";

        EmailSendResponse response = resendEmailService.sendEmail(request.getTo(), request.getSubject(), html);
        return ResponseEntity.ok(ApiResponse.ok(response.getMessage(), response));
    }

    @PostMapping("/booking-confirmation")
    @Operation(summary = "Send booking confirmation email ticket", description = "Sends an HTML bus ticket confirmation to traveller's email via Resend")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendBookingConfirmation(
            @Valid @RequestBody BookingConfirmationEmailRequest request) {
        log.info("Dispatching booking confirmation ticket email to [{}] for ref [{}]", request.getToEmail(), request.getBookingReference());
        EmailSendResponse response = resendEmailService.sendBookingConfirmationEmail(request);

        if ("FAILED".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMessage(), response));
        }

        return ResponseEntity.ok(ApiResponse.ok(response.getMessage(), response));
    }
}
