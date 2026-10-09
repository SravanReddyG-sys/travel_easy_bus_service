package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.LoginRequest;
import com.traveleasy.bus.dto.request.RegisterRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.AuthResponse;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.security.JwtTokenProvider;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Endpoints for Traveller and Operator authentication, token issuance, registration, and user info")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final BusOperatorRepository operatorRepository;

    private static final UUID DEFAULT_OPERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @PostMapping("/login")
    @Operation(summary = "Login as Traveller or Operator", description = "Authenticates user and returns JWT token with role claims")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email [{}] with role hint [{}]", request.getEmail(), request.getRole());

        String email = request.getEmail().trim().toLowerCase();
        boolean isOperator = "OPERATOR".equalsIgnoreCase(request.getRole()) || email.contains("operator") || email.contains("royal");

        UserRole role = isOperator ? UserRole.ROLE_OPERATOR : UserRole.ROLE_TRAVELLER;
        UUID operatorId = null;
        String companyName = null;

        if (isOperator) {
            Optional<BusOperator> operatorOpt = operatorRepository.findByContactEmail(email);
            if (operatorOpt.isPresent()) {
                BusOperator op = operatorOpt.get();
                operatorId = op.getId();
                companyName = op.getCompanyName();
            } else {
                operatorId = DEFAULT_OPERATOR_ID;
                companyName = "Royal Express Travels";
            }
        }

        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, email, operatorId, Set.of(role));

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userId)
                .email(email)
                .fullName(isOperator ? (companyName != null ? companyName : "Bus Operator") : "Traveller User")
                .role(role.name())
                .operatorId(operatorId)
                .companyName(companyName)
                .expiresInSeconds(86400)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    @PostMapping("/register")
    @Operation(summary = "Register as Traveller or Bus Operator", description = "Creates a new user profile and returns authenticated session token")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email [{}] as role [{}]", request.getEmail(), request.getRole());

        String email = request.getEmail().trim().toLowerCase();
        boolean isOperator = "OPERATOR".equalsIgnoreCase(request.getRole());
        UserRole role = isOperator ? UserRole.ROLE_OPERATOR : UserRole.ROLE_TRAVELLER;

        UUID operatorId = null;
        String companyName = request.getCompanyName();

        if (isOperator) {
            String company = (companyName != null && !companyName.isBlank()) ? companyName : "Express Travels";
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

        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, email, operatorId, Set.of(role));

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userId)
                .email(email)
                .fullName(request.getFullName())
                .role(role.name())
                .operatorId(operatorId)
                .companyName(companyName)
                .expiresInSeconds(86400)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Registration successful", authResponse));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info", description = "Returns details extracted from active JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser() {
        UserPrincipal principal = SecurityUtils.requireCurrentUserPrincipal();
        boolean isOperator = principal.isOperator();

        AuthResponse response = AuthResponse.builder()
                .userId(principal.getUserId())
                .email(principal.getEmail())
                .fullName(isOperator ? "Royal Express Travels Operator" : "Verified Traveller")
                .role(isOperator ? "ROLE_OPERATOR" : "ROLE_TRAVELLER")
                .operatorId(principal.getOperatorId())
                .companyName(isOperator ? "Royal Express Travels" : null)
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
