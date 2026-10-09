package com.traveleasy.bus.security;

import com.traveleasy.bus.exception.ForbiddenAccessException;
import com.traveleasy.bus.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldThrowUnauthorizedWhenNoAuthenticationPresent() {
        assertThrows(UnauthorizedException.class, SecurityUtils::requireCurrentUserPrincipal);
        assertThrows(UnauthorizedException.class, SecurityUtils::requireCurrentUserId);
    }

    @Test
    void shouldExtractPrincipalAndEnforceOperatorOwnership() {
        UUID operatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .email("operator@traveleasy.com")
                .operatorId(operatorId)
                .roles(Set.of(UserRole.ROLE_OPERATOR))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(userId, SecurityUtils.requireCurrentUserId());
        assertEquals(operatorId, SecurityUtils.requireCurrentOperatorId());
        assertTrue(SecurityUtils.isOperator());
        assertFalse(SecurityUtils.isAdmin());

        // Same operator matches successfully
        assertDoesNotThrow(() -> SecurityUtils.verifyOperatorOwnership(operatorId));

        // Different operator should fail with ForbiddenAccessException
        UUID differentOperatorId = UUID.randomUUID();
        assertThrows(ForbiddenAccessException.class, () -> SecurityUtils.verifyOperatorOwnership(differentOperatorId));
    }

    @Test
    void shouldAllowAdminToBypassOperatorOwnershipCheck() {
        UUID adminUserId = UUID.randomUUID();

        UserPrincipal adminPrincipal = UserPrincipal.builder()
                .userId(adminUserId)
                .email("admin@traveleasy.com")
                .roles(Set.of(UserRole.ROLE_ADMIN))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtils.isAdmin());
        // Admin should be able to operate on any operator resource
        assertDoesNotThrow(() -> SecurityUtils.verifyOperatorOwnership(UUID.randomUUID()));
    }
}
