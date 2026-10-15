package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.LoginRequest;
import com.traveleasy.bus.dto.request.RegisterRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.AuthResponse;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.User;
import com.traveleasy.bus.repository.BusOperatorRepository;
import com.traveleasy.bus.repository.UserRepository;
import com.traveleasy.bus.security.JwtTokenProvider;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.security.UserPrincipal;
import com.traveleasy.bus.security.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Endpoints for Traveller and Operator real database authentication, token issuance, registration, and user info")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final BusOperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
