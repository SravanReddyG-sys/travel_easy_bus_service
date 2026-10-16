package com.traveleasy.bus.service.email;

import com.traveleasy.bus.dto.request.BookingConfirmationEmailRequest;
import com.traveleasy.bus.dto.response.EmailSendResponse;

public interface ResendEmailService {

    /**
     * Send arbitrary email via Resend API
     */
    EmailSendResponse sendEmail(String toEmail, String subject, String htmlContent);

    /**
     * Send branded welcome email upon user registration
     */
    EmailSendResponse sendWelcomeEmail(String toEmail, String fullName, String role);

    /**
     * Send bus booking confirmation email ticket
     */
    EmailSendResponse sendBookingConfirmationEmail(BookingConfirmationEmailRequest request);

    /**
     * Send 6-digit email verification OTP upon registration
     */
    EmailSendResponse sendVerificationOtpEmail(String toEmail, String fullName, String otpCode);

    /**
     * Send 6-digit password reset OTP
     */
    EmailSendResponse sendPasswordResetOtpEmail(String toEmail, String fullName, String otpCode);

    /**
     * Returns true if Resend API key is configured
     */
    boolean isConfigured();

    /**
     * Get sender email address
     */
    String getSenderEmail();
}
