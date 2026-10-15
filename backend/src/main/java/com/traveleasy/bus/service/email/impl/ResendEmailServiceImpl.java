package com.traveleasy.bus.service.email.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.BookingConfirmationEmailRequest;
import com.traveleasy.bus.dto.response.EmailSendResponse;
import com.traveleasy.bus.service.email.ResendEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ResendEmailServiceImpl implements ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String apiKey;

    @Value("${resend.from-email:${RESEND_FROM_EMAIL:TravelEasy <onboarding@resend.dev>}}")
    private String fromEmail;

    @Value("${resend.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty() && !apiKey.trim().equalsIgnoreCase("dummy");
    }

    @Override
    public String getSenderEmail() {
        return fromEmail;
    }

    @Override
    public EmailSendResponse sendEmail(String toEmail, String subject, String htmlContent) {
        if (!isConfigured()) {
            log.info("[Resend Simulated] API key not configured. Email to [{}] with subject '{}' simulated.", toEmail, subject);
            return EmailSendResponse.builder()
                    .id("sim_" + UUID.randomUUID())
                    .status("SIMULATED")
                    .recipient(toEmail)
                    .subject(subject)
                    .message("Resend API key not configured. Email delivery simulated successfully. Set RESEND_API_KEY to deliver real emails.")
                    .resendConfigured(false)
                    .build();
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", fromEmail);
            payload.put("to", List.of(toEmail));
            payload.put("subject", subject);
            payload.put("html", htmlContent);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("[Resend] Sending email to [{}] with subject '{}'...", toEmail, subject);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                String emailId = "resend_" + UUID.randomUUID();
                try {
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("id")) {
                        emailId = node.get("id").asText();
                    }
                } catch (Exception ignored) {}

                log.info("[Resend] Email successfully sent to [{}] with Resend ID [{}]", toEmail, emailId);
                return EmailSendResponse.builder()
                        .id(emailId)
                        .status("SENT")
                        .recipient(toEmail)
                        .subject(subject)
                        .message("Email sent successfully via Resend API")
                        .resendConfigured(true)
                        .build();
            } else {
                log.warn("[Resend] Failed with status [{}]: {}", statusCode, responseBody);
                String friendlyMessage = "Resend returned HTTP " + statusCode + ": " + responseBody;
                if ((statusCode == 403 || statusCode == 422) && responseBody != null && responseBody.contains("only send testing emails")) {
                    friendlyMessage = "Email delivery failed: Resend is currently in free sandbox mode (using onboarding@resend.dev) and requires a verified domain to deliver to external email addresses like '" + toEmail + "'. To send emails to all users, verify a domain at resend.com/domains.";
                }

                return EmailSendResponse.builder()
                        .id("err_" + UUID.randomUUID())
                        .status("FAILED")
                        .recipient(toEmail)
                        .subject(subject)
                        .message(friendlyMessage)
                        .resendConfigured(true)
                        .build();
            }
        } catch (Exception e) {
            log.error("[Resend] Exception sending email to [{}]: {}", toEmail, e.getMessage(), e);
            return EmailSendResponse.builder()
                    .id("err_" + UUID.randomUUID())
                    .status("FAILED")
                    .recipient(toEmail)
                    .subject(subject)
                    .message("Failed to dispatch email: " + e.getMessage())
                    .resendConfigured(true)
                    .build();
        }
    }

    @Override
    public EmailSendResponse sendWelcomeEmail(String toEmail, String fullName, String role) {
        String greetingName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "Traveler";
        String roleLabel = "OPERATOR".equalsIgnoreCase(role) ? "Bus Operator" : "Traveler";
        String subject = "Welcome to TravelEasy, " + greetingName + "! 🚌";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Welcome to TravelEasy</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 24px;">
                  <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); padding: 32px 24px; text-align: center; color: #ffffff;">
                      <h1 style="margin: 0; font-size: 26px; font-weight: 700; letter-spacing: -0.5px;">🚌 TravelEasy</h1>
                      <p style="margin: 8px 0 0; opacity: 0.9; font-size: 15px;">Smart Bus Reservation Platform</p>
                    </div>
                    <div style="padding: 32px 24px; color: #333333; line-height: 1.6;">
                      <h2 style="margin-top: 0; color: #1e3c72; font-size: 20px;">Welcome aboard, {{NAME}}!</h2>
                      <p>Your account has been successfully created with the role of <strong>{{ROLE}}</strong>.</p>
                      
                      <div style="background: #f8fafc; border-left: 4px solid #2a5298; padding: 16px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 0; font-size: 14px; color: #475569;">
                          <strong>Registered Email:</strong> {{EMAIL}}<br>
                          <strong>Account Type:</strong> {{ROLE}}<br>
                          <strong>Platform Access:</strong> Instant live seat booking across 10 major intercity routes.
                        </p>
                      </div>

                      <h3 style="color: #1e3c72; font-size: 16px; margin-top: 24px;">What You Can Do Now:</h3>
                      <ul style="color: #4b5563; font-size: 14px; padding-left: 20px;">
                        <li>Search live daily bus schedules across 10 popular routes.</li>
                        <li>Pick your preferred deck (Lower/Upper) and lock seats in real time.</li>
                        <li>Manage your held and confirmed tickets directly in <strong>My Bookings</strong>.</li>
                      </ul>

                      <div style="text-align: center; margin: 32px 0 16px;">
                        <a href="http://localhost:4200" style="background: #2a5298; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: 600; font-size: 15px; display: inline-block;">Explore & Book Buses</a>
                      </div>
                    </div>
                    <div style="background: #f8fafc; padding: 18px 24px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;">
                      TravelEasy Bus Service Platform • SAA Microservice Project<br>
                      Transactional Email powered by Resend
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("{{NAME}}", greetingName)
                .replace("{{ROLE}}", roleLabel)
                .replace("{{EMAIL}}", toEmail);

        return sendEmail(toEmail, subject, html);
    }

    @Override
    public EmailSendResponse sendBookingConfirmationEmail(BookingConfirmationEmailRequest request) {
        String name = (request.getPassengerName() != null && !request.getPassengerName().isBlank())
                ? request.getPassengerName().trim()
                : "Traveler";
        String ref = request.getBookingReference() != null ? request.getBookingReference() : "TE-BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String origin = request.getOrigin() != null ? request.getOrigin() : "Origin";
        String destination = request.getDestination() != null ? request.getDestination() : "Destination";
        String travelDate = request.getTravelDate() != null ? request.getTravelDate() : "October 2026";
        String departureTime = request.getDepartureTime() != null ? request.getDepartureTime() : "Scheduled Time";
        String busNumber = request.getBusNumber() != null ? request.getBusNumber() : "Express Coach";
        String operatorName = request.getOperatorName() != null ? request.getOperatorName() : "Royal Express Travels";
        String seats = (request.getSeats() != null && !request.getSeats().isEmpty()) ? String.join(", ", request.getSeats()) : "1A";
        String amount = request.getTotalAmount() != null ? "₹" + request.getTotalAmount() : "₹472.50";
        String boarding = request.getBoardingPoint() != null ? request.getBoardingPoint() : "Main Boarding Terminal";
        String dropping = request.getDroppingPoint() != null ? request.getDroppingPoint() : "City Center Terminal";

        String subject = "Booking Confirmation: " + origin + " to " + destination + " [" + ref + "] 🎫";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Booking Confirmation</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px;">
                  <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #059669 0%, #10b981 100%); padding: 24px; text-align: center; color: #ffffff;">
                      <h2 style="margin: 0; font-size: 22px; font-weight: 700;">🎫 Booking Confirmed!</h2>
                      <p style="margin: 6px 0 0; opacity: 0.95; font-size: 14px;">Reference: <strong>{{REF}}</strong></p>
                    </div>

                    <div style="padding: 24px; color: #1e293b;">
                      <p style="margin-top: 0; font-size: 15px;">Hello <strong>{{NAME}}</strong>, your bus ticket has been held and confirmed.</p>
                      
                      <!-- Journey Details Box -->
                      <div style="border: 1px solid #cbd5e1; border-radius: 8px; padding: 18px; margin: 18px 0; background: #f8fafc;">
                        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; margin-bottom: 12px;">
                          <div>
                            <span style="font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 600;">ROUTE</span>
                            <div style="font-size: 18px; font-weight: 700; color: #0f172a;">{{ORIGIN}} ➔ {{DESTINATION}}</div>
                          </div>
                          <div style="text-align: right;">
                            <span style="font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 600;">DATE</span>
                            <div style="font-size: 15px; font-weight: 600; color: #0f172a;">{{DATE}}</div>
                          </div>
                        </div>

                        <table style="width: 100%; font-size: 14px; border-collapse: collapse;">
                          <tr>
                            <td style="padding: 6px 0; color: #64748b;">Departure Time:</td>
                            <td style="padding: 6px 0; font-weight: 600; color: #0f172a; text-align: right;">{{TIME}}</td>
                          </tr>
                          <tr>
                            <td style="padding: 6px 0; color: #64748b;">Bus Operator:</td>
                            <td style="padding: 6px 0; font-weight: 600; color: #0f172a; text-align: right;">{{OPERATOR}} ({{BUSNUM}})</td>
                          </tr>
                          <tr>
                            <td style="padding: 6px 0; color: #64748b;">Seat Number(s):</td>
                            <td style="padding: 6px 0; font-weight: 700; color: #059669; text-align: right;">{{SEATS}}</td>
                          </tr>
                          <tr>
                            <td style="padding: 6px 0; color: #64748b;">Boarding Point:</td>
                            <td style="padding: 6px 0; font-weight: 500; color: #0f172a; text-align: right;">{{BOARDING}}</td>
                          </tr>
                          <tr>
                            <td style="padding: 6px 0; color: #64748b;">Dropping Point:</td>
                            <td style="padding: 6px 0; font-weight: 500; color: #0f172a; text-align: right;">{{DROPPING}}</td>
                          </tr>
                          <tr style="border-top: 1px dashed #cbd5e1;">
                            <td style="padding: 10px 0 0; font-size: 16px; font-weight: 700; color: #0f172a;">Total Fare Paid:</td>
                            <td style="padding: 10px 0 0; font-size: 18px; font-weight: 800; color: #059669; text-align: right;">{{AMOUNT}}</td>
                          </tr>
                        </table>
                      </div>

                      <div style="background: #eff6ff; border-left: 4px solid #3b82f6; padding: 12px 16px; border-radius: 4px; font-size: 13px; color: #1e40af;">
                        💡 <strong>Travel Tip:</strong> Please arrive at the boarding point 15 minutes before scheduled departure and carry a valid photo ID.
                      </div>

                      <div style="text-align: center; margin: 24px 0 8px;">
                        <a href="http://localhost:4200/traveller/bookings" style="background: #0f172a; color: #ffffff; text-decoration: none; padding: 10px 22px; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block;">View in My Bookings</a>
                      </div>
                    </div>

                    <div style="background: #f8fafc; padding: 16px 24px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;">
                      TravelEasy Bus Service • Powered by Resend Email API<br>
                      Need help? Contact support@traveleasy.com
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("{{REF}}", ref)
                .replace("{{NAME}}", name)
                .replace("{{ORIGIN}}", origin)
                .replace("{{DESTINATION}}", destination)
                .replace("{{DATE}}", travelDate)
                .replace("{{TIME}}", departureTime)
                .replace("{{OPERATOR}}", operatorName)
                .replace("{{BUSNUM}}", busNumber)
                .replace("{{SEATS}}", seats)
                .replace("{{BOARDING}}", boarding)
                .replace("{{DROPPING}}", dropping)
                .replace("{{AMOUNT}}", amount);

        return sendEmail(request.getToEmail(), subject, html);
    }

    @Override
    public EmailSendResponse sendVerificationOtpEmail(String toEmail, String fullName, String otpCode) {
        String greetingName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "Traveler";
        String subject = "Your TravelEasy Verification Code: " + otpCode + " 🔐";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Verify Your Email</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
                  <div style="max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); padding: 28px; text-align: center; color: #ffffff;">
                      <h1 style="margin: 0; font-size: 24px; font-weight: 700;">🚌 TravelEasy</h1>
                      <p style="margin: 6px 0 0; opacity: 0.9; font-size: 14px;">Email Account Verification</p>
                    </div>

                    <div style="padding: 30px 24px; color: #1e293b; text-align: center;">
                      <p style="margin-top: 0; font-size: 16px;">Hello <strong>{{NAME}}</strong>,</p>
                      <p style="color: #64748b; font-size: 14px; margin-bottom: 24px;">
                        Thank you for registering on TravelEasy! Please use the 6-digit verification code below to verify your email address:
                      </p>

                      <!-- OTP Box -->
                      <div style="background: #f1f5f9; border: 2px dashed #cbd5e1; border-radius: 10px; padding: 18px 24px; margin: 24px auto; display: inline-block; min-width: 200px;">
                        <span style="font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #1e3c72; font-family: monospace;">{{OTP}}</span>
                      </div>

                      <p style="color: #64748b; font-size: 13px; margin: 16px 0 0;">
                        ⏱️ This code will expire in <strong>10 minutes</strong>.<br>
                        If you did not request this registration, please disregard this email.
                      </p>
                    </div>

                    <div style="background: #f8fafc; padding: 16px 24px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;">
                      TravelEasy Bus Service Platform • Powered by Resend Email API
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("{{NAME}}", greetingName)
                .replace("{{OTP}}", otpCode);

        return sendEmail(toEmail, subject, html);
    }

    @Override
    public EmailSendResponse sendPasswordResetOtpEmail(String toEmail, String fullName, String otpCode) {
        String greetingName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "Traveler";
        String subject = "TravelEasy Password Reset Code: " + otpCode + " 🔑";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Reset Your Password</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
                  <div style="max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #b91c1c 0%, #dc2626 100%); padding: 28px; text-align: center; color: #ffffff;">
                      <h1 style="margin: 0; font-size: 24px; font-weight: 700;">🔑 TravelEasy</h1>
                      <p style="margin: 6px 0 0; opacity: 0.9; font-size: 14px;">Password Reset Request</p>
                    </div>

                    <div style="padding: 30px 24px; color: #1e293b; text-align: center;">
                      <p style="margin-top: 0; font-size: 16px;">Hello <strong>{{NAME}}</strong>,</p>
                      <p style="color: #64748b; font-size: 14px; margin-bottom: 24px;">
                        We received a request to reset the password for your TravelEasy account. Use the 6-digit security code below:
                      </p>

                      <!-- OTP Box -->
                      <div style="background: #fef2f2; border: 2px dashed #fca5a5; border-radius: 10px; padding: 18px 24px; margin: 24px auto; display: inline-block; min-width: 200px;">
                        <span style="font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #b91c1c; font-family: monospace;">{{OTP}}</span>
                      </div>

                      <p style="color: #64748b; font-size: 13px; margin: 16px 0 0;">
                        ⏱️ This reset code is valid for <strong>10 minutes</strong>.<br>
                        🔒 If you did not make this request, you can safely ignore this email. Your password will remain unchanged.
                      </p>
                    </div>

                    <div style="background: #f8fafc; padding: 16px 24px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;">
                      TravelEasy Bus Service Platform • Powered by Resend Email API
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("{{NAME}}", greetingName)
                .replace("{{OTP}}", otpCode);

        return sendEmail(toEmail, subject, html);
    }
}
