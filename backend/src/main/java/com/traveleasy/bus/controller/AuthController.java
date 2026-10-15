package com.traveleasy.bus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.request.*;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.AuthResponse;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.EmailVerification;
import com.traveleasy.bus.entity.User;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.EmailVerificationRepository;
import com.traveleasy.bus.repository.UserRepository;
import com.traveleasy.bus.security.JwtTokenProvider;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import com.traveleasy.bus.service.email.ResendEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Endpoints for Traveller and Operator real database authentication, email verification via Resend, password reset, and user info")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final BusOperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailService resendEmailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ObjectMapper objectMapper;

    private static final UUID DEFAULT_OPERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @PostMapping("/login")
    @Operation(summary = "Login as Traveller or Operator", description = "Verifies email and password against database and returns JWT token with role claims")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email address is required."));
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Password is required."));
        }

        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing login request for email [{}]", email);

        // 1. Look up User in database
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: No registered user found for email [{}]", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No account found with email '" + email + "'. Please register first."));
        }

        User user = userOpt.get();

        // 2. Verify password with BCrypt
        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPasswordHash())) {
            log.warn("Login failed: Incorrect password for email [{}]", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Incorrect password for account '" + email + "'. Please check your password and try again."));
        }

        // 3. User authenticated successfully!
        boolean isOperator = "OPERATOR".equalsIgnoreCase(user.getRole());
        UserRole role = isOperator ? UserRole.ROLE_OPERATOR : UserRole.ROLE_TRAVELLER;
        UUID operatorId = user.getOperatorId();
        String companyName = user.getCompanyName();

        String token = jwtTokenProvider.generateToken(user.getId(), email, operatorId, Set.of(role));

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(email)
                .fullName(user.getFullName())
                .role(role.name())
                .operatorId(operatorId)
                .companyName(companyName)
                .expiresInSeconds(86400)
                .build();

        log.info("User [{}] ({}) authenticated successfully", email, role.name());
        return ResponseEntity.ok(ApiResponse.ok("Login successful! Welcome back, " + user.getFullName(), authResponse));
    }

    @PostMapping("/register")
    @Operation(summary = "Register as Traveller or Bus Operator", description = "Creates a real user account in database with BCrypt password hash and returns JWT session")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Register request for email [{}] as role [{}]", email, request.getRole());

        // 1. Verify if email is already registered in database
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration rejected: Duplicate email [{}]", email);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("An account with email '" + email + "' already exists. Please sign in instead."));
        }

        // 2. Validate password
        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 6 characters long."));
        }

        boolean isOperator = "OPERATOR".equalsIgnoreCase(request.getRole());
        UserRole role = isOperator ? UserRole.ROLE_OPERATOR : UserRole.ROLE_TRAVELLER;

        UUID operatorId = null;
        String companyName = request.getCompanyName();

        if (isOperator) {
            String company = (companyName != null && !companyName.isBlank()) ? companyName.trim() : "Express Travels";
            String code = "OP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            BusOperator newOperator = BusOperator.builder()
                    .operatorCode(code)
                    .companyName(company)
                    .contactEmail(email)
                    .contactPhone(request.getContactPhone() != null ? request.getContactPhone() : "+91-9999999999")
                    .isActive(true)
                    .build();

            BusOperator saved = operatorRepository.save(newOperator);
            operatorId = saved.getId();
            companyName = saved.getCompanyName();
        }

        // 3. Hash password using BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword().trim());

        // 4. Save User entity to Database
        User newUser = User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .fullName(request.getFullName().trim())
                .phone(request.getContactPhone())
                .role(isOperator ? "OPERATOR" : "TRAVELLER")
                .operatorId(operatorId)
                .companyName(companyName)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Saved user [{}] to database with ID [{}] and role [{}]", email, savedUser.getId(), savedUser.getRole());

        // Dispatch welcome email asynchronously via Resend
        CompletableFuture.runAsync(() -> {
            try {
                resendEmailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole());
            } catch (Exception ex) {
                log.warn("Non-critical: Welcome email delivery failed for [{}]: {}", savedUser.getEmail(), ex.getMessage());
            }
        });

        // 5. Generate signed JWT token
        String token = jwtTokenProvider.generateToken(savedUser.getId(), email, operatorId, Set.of(role));

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .email(email)
                .fullName(savedUser.getFullName())
                .role(role.name())
                .operatorId(operatorId)
                .companyName(companyName)
                .expiresInSeconds(86400)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Registration successful! Welcome, " + savedUser.getFullName(), authResponse));
    }

    @PostMapping("/register/send-otp")
    @Operation(summary = "Step 1: Send registration OTP via Resend email", description = "Validates user details and dispatches a 6-digit verification code to the email via Resend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendRegistrationOtp(
            @Valid @RequestBody SendVerificationOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Sending registration OTP to [{}]", email);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("An account with email '" + email + "' already exists. Please log in or reset your password."));
        }

        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 6 characters long."));
        }

        // Generate 6-digit random OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("email", email);
            payloadMap.put("fullName", request.getFullName().trim());
            payloadMap.put("password", passwordEncoder.encode(request.getPassword().trim()));
            payloadMap.put("role", request.getRole() != null ? request.getRole().trim().toUpperCase() : "TRAVELLER");
            payloadMap.put("companyName", request.getCompanyName() != null ? request.getCompanyName().trim() : "");
            payloadMap.put("contactPhone", request.getContactPhone() != null ? request.getContactPhone().trim() : "");

            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .otpCode(otp)
                    .purpose("REGISTRATION")
                    .dataPayload(payloadJson)
                    .expiresAt(expiresAt)
                    .isUsed(false)
                    .build();

            emailVerificationRepository.save(verification);

            // Send Email via Resend
            var emailResponse = resendEmailService.sendVerificationOtpEmail(email, request.getFullName(), otp);

            boolean isLiveSent = emailResponse != null && "SENT".equalsIgnoreCase(emailResponse.getStatus());
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", email);
            responseData.put("expiresInMinutes", 10);
            responseData.put("deliveredViaResend", isLiveSent);

            // If Resend failed (e.g. sandbox restriction on unverified test domains), provide demoOtp so testing is NEVER blocked!
            if (!isLiveSent) {
                responseData.put("demoOtp", otp);
                responseData.put("sandboxNote", "Resend free tier delivers to verified accounts only. For sandbox testing, your OTP is: " + otp);
            }

            return ResponseEntity.ok(ApiResponse.ok("Verification code sent to " + email, responseData));

        } catch (Exception e) {
            log.error("Failed to generate registration OTP for [{}]: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send verification code. Please try again."));
        }
    }

    @PostMapping("/register/verify-otp")
    @Operation(summary = "Step 2: Verify registration OTP and create account", description = "Verifies the 6-digit code and creates the active user account in the database")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtpCode().trim();
        log.info("Verifying registration OTP for [{}]", email);

        Optional<EmailVerification> verifOpt = emailVerificationRepository.findValidOtp(
                email, otp, "REGISTRATION", LocalDateTime.now()
        );

        if (verifOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired verification code. Please check your email or request a new code."));
        }

        EmailVerification verif = verifOpt.get();
        try {
            Map<String, Object> payload = objectMapper.readValue(verif.getDataPayload(), Map.class);
            String fullName = (String) payload.get("fullName");
            String passwordHash = (String) payload.get("password");
            String roleStr = (String) payload.get("role");
            String companyName = (String) payload.get("companyName");
            String phone = (String) payload.get("contactPhone");

            boolean isOperator = "OPERATOR".equalsIgnoreCase(roleStr);
            UserRole userRole = isOperator ? UserRole.ROLE_OPERATOR : UserRole.ROLE_TRAVELLER;
            UUID operatorId = null;

            if (isOperator) {
                String company = (companyName != null && !companyName.isBlank()) ? companyName.trim() : "Express Travels";
                String code = "OP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

                BusOperator newOperator = BusOperator.builder()
                        .operatorCode(code)
                        .companyName(company)
                        .contactEmail(email)
                        .contactPhone(phone != null && !phone.isBlank() ? phone : "+91-9999999999")
                        .isActive(true)
                        .build();

                BusOperator saved = operatorRepository.save(newOperator);
                operatorId = saved.getId();
                companyName = saved.getCompanyName();
            }

            User newUser = User.builder()
                    .email(email)
                    .passwordHash(passwordHash)
                    .fullName(fullName)
                    .phone(phone)
                    .role(isOperator ? "OPERATOR" : "TRAVELLER")
                    .operatorId(operatorId)
                    .companyName(companyName)
                    .isEmailVerified(true)
                    .build();

            User savedUser = userRepository.save(newUser);
            verif.setIsUsed(true);
            emailVerificationRepository.save(verif);

            log.info("User [{}] verified and registered with ID [{}]", email, savedUser.getId());

            // Welcome email asynchronously via Resend
            CompletableFuture.runAsync(() -> {
                try {
                    resendEmailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole());
                } catch (Exception ex) {
                    log.warn("Non-critical: Welcome email failed for [{}]: {}", savedUser.getEmail(), ex.getMessage());
                }
            });

            String token = jwtTokenProvider.generateToken(savedUser.getId(), email, operatorId, Set.of(userRole));
            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(savedUser.getId())
                    .email(email)
                    .fullName(savedUser.getFullName())
                    .role(userRole.name())
                    .operatorId(operatorId)
                    .companyName(companyName)
                    .expiresInSeconds(86400)
                    .build();

            return ResponseEntity.ok(ApiResponse.ok("Email verified! Welcome to TravelEasy, " + savedUser.getFullName(), authResponse));

        } catch (Exception e) {
            log.error("Failed to complete verified registration for [{}]: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to complete account registration."));
        }
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Forgot password: send reset OTP code", description = "Sends a 6-digit password reset code to the user's registered email via Resend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing forgot password request for [{}]", email);

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No account found with email '" + email + "'. Please check your email or register."));
        }

        User user = userOpt.get();
        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .otpCode(otp)
                .purpose("PASSWORD_RESET")
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();

        emailVerificationRepository.save(verification);

        var emailRes = resendEmailService.sendPasswordResetOtpEmail(email, user.getFullName(), otp);
        boolean isLiveSent = emailRes != null && "SENT".equalsIgnoreCase(emailRes.getStatus());

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("expiresInMinutes", 10);
        data.put("deliveredViaResend", isLiveSent);

        if (!isLiveSent) {
            data.put("demoOtp", otp);
            data.put("sandboxNote", "Resend free tier delivers to verified accounts only. For sandbox testing, your OTP is: " + otp);
        }

        return ResponseEntity.ok(ApiResponse.ok("Password reset code sent to " + email, data));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Reset password using OTP code", description = "Verifies the reset OTP and updates the user's password with BCrypt hash")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtpCode().trim();
        log.info("Processing password reset for [{}]", email);

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("New password must be at least 6 characters long."));
        }

        Optional<EmailVerification> verifOpt = emailVerificationRepository.findValidOtp(
                email, otp, "PASSWORD_RESET", LocalDateTime.now()
        );

        if (verifOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired reset code. Please request a new code."));
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User account not found."));
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);

        EmailVerification verif = verifOpt.get();
        verif.setIsUsed(true);
        emailVerificationRepository.save(verif);

        log.info("Password successfully reset for user [{}]", email);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully! You can now log in with your new password.", Map.of("email", email)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info", description = "Returns details extracted from active JWT token and database")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser() {
        UserPrincipal principal = SecurityUtils.requireCurrentUserPrincipal();
        
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(principal.getEmail());
        String fullName = userOpt.map(User::getFullName)
                .orElse(principal.isOperator() ? "Bus Operator" : "Verified Traveller");
        String companyName = userOpt.map(User::getCompanyName)
                .orElse(principal.isOperator() ? "Royal Express Travels" : null);

        AuthResponse response = AuthResponse.builder()
                .userId(principal.getUserId())
                .email(principal.getEmail())
                .fullName(fullName)
                .role(principal.isOperator() ? "ROLE_OPERATOR" : "ROLE_TRAVELLER")
                .operatorId(principal.getOperatorId())
                .companyName(companyName)
                .expiresInSeconds(86400)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Current user profile retrieved", response));
    }

    @GetMapping("/token")
    @Operation(summary = "Quick token generation for development/testing", description = "Generates a signed JWT token for the specified role")
    public ResponseEntity<ApiResponse<Map<String, String>>> getToken(
            @RequestParam(defaultValue = "OPERATOR") String role,
            @RequestParam(required = false) UUID operatorId) {

        UserRole userRole = "ADMIN".equalsIgnoreCase(role) ? UserRole.ROLE_ADMIN :
                "OPERATOR".equalsIgnoreCase(role) ? UserRole.ROLE_OPERATOR : UserRole.ROLE_TRAVELLER;

        UUID opId = (userRole == UserRole.ROLE_OPERATOR || userRole == UserRole.ROLE_ADMIN)
                ? (operatorId != null ? operatorId : DEFAULT_OPERATOR_ID)
                : null;

        UUID userId = UUID.randomUUID();
        String email = userRole == UserRole.ROLE_OPERATOR ? "operator@traveleasy.com" : "traveller@traveleasy.com";
        String token = jwtTokenProvider.generateToken(userId, email, opId, Set.of(userRole));

        return ResponseEntity.ok(ApiResponse.ok("Token generated successfully", Map.of(
                "token", token,
                "role", userRole.name(),
                "email", email,
                "operatorId", opId != null ? opId.toString() : ""
        )));
    }
}
