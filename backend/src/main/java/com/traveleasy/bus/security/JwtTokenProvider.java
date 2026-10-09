package com.traveleasy.bus.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${app.security.jwt.issuer:traveleasy-auth-service}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public UserPrincipal extractUserPrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = null;
        try {
            if (claims.getSubject() != null) {
                userId = UUID.fromString(claims.getSubject());
            }
        } catch (IllegalArgumentException e) {
            log.debug("JWT subject is not a UUID: {}", claims.getSubject());
        }

        String email = claims.get("email", String.class);
        
        UUID operatorId = null;
        String operatorIdStr = claims.get("operatorId", String.class);
        if (operatorIdStr != null && !operatorIdStr.isBlank()) {
            try {
                operatorId = UUID.fromString(operatorIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid operatorId UUID format in JWT: {}", operatorIdStr);
            }
        }

        Set<UserRole> roles = new HashSet<>();
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> roleList) {
            for (Object r : roleList) {
                if (r != null) {
                    roles.add(UserRole.fromString(r.toString()));
                }
            }
        } else if (rolesObj instanceof String roleStr) {
            roles.add(UserRole.fromString(roleStr));
        }

        if (roles.isEmpty()) {
            roles.add(UserRole.ROLE_TRAVELLER);
        }

        return UserPrincipal.builder()
                .userId(userId)
                .email(email)
                .operatorId(operatorId)
                .roles(roles)
                .build();
    }

    /**
     * Helper to generate tokens for testing/development.
     */
    public String generateToken(UUID userId, String email, UUID operatorId, Set<UserRole> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        List<String> roleStrings = roles.stream().map(UserRole::name).collect(Collectors.toList());

        var builder = Jwts.builder()
                .subject(userId != null ? userId.toString() : UUID.randomUUID().toString())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("roles", roleStrings)
                .signWith(secretKey);

        if (email != null) {
            builder.claim("email", email);
        }
        if (operatorId != null) {
            builder.claim("operatorId", operatorId.toString());
        }

        return builder.compact();
    }
}
