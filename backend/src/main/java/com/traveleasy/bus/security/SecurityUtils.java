package com.traveleasy.bus.security;

import com.traveleasy.bus.exception.ForbiddenAccessException;
import com.traveleasy.bus.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
        // Private constructor for utility class
    }

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal requireCurrentUserPrincipal() {
        return getCurrentUserPrincipal()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required to perform this action"));
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getUserId);
    }

    public static UUID requireCurrentUserId() {
        return requireCurrentUserPrincipal().getUserId();
    }

    public static Optional<UUID> getCurrentOperatorId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getOperatorId);
    }

    public static UUID requireCurrentOperatorId() {
        UserPrincipal principal = requireCurrentUserPrincipal();
        if (principal.getOperatorId() == null) {
            throw new ForbiddenAccessException("Authenticated user is not associated with any bus operator");
        }
        return principal.getOperatorId();
    }

    public static boolean isOperator() {
        return getCurrentUserPrincipal().map(UserPrincipal::isOperator).orElse(false);
    }

    public static boolean isAdmin() {
        return getCurrentUserPrincipal().map(UserPrincipal::isAdmin).orElse(false);
    }

    /**
     * Strict operator ownership verification:
     * Admins bypass operator check. Operators must match the resource's owner operatorId.
     */
    public static void verifyOperatorOwnership(UUID resourceOperatorId) {
        UserPrincipal principal = requireCurrentUserPrincipal();
        if (principal.isAdmin()) {
            return; // Admins have global operational access
        }

        if (principal.getOperatorId() == null || resourceOperatorId == null || !principal.getOperatorId().equals(resourceOperatorId)) {
            throw ForbiddenAccessException.operatorMismatch();
        }
    }
}
