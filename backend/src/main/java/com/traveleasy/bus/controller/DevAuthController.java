package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.security.JwtTokenProvider;
import com.traveleasy.bus.security.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Development Authentication Helper", description = "Development utility to issue valid signed JWTs for UI demo and testing")
public class DevAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    private static final UUID DEFAULT_OPERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @GetMapping("/token")
    @Operation(summary = "Generate development JWT token", description = "Generates a signed JWT token for the specified role and operator")
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

        return ResponseEntity.ok(ApiResponse.ok("Development token generated successfully", Map.of(
                "token", token,
                "role", userRole.name(),
                "email", email,
                "operatorId", opId != null ? opId.toString() : ""
        )));
    }
}
