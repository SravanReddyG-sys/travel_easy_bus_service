package com.traveleasy.bus.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "TestSecretKeyForJwtTokenProviderTestingThatIsVeryLongAndSecure256Bits!";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, 3600000, "traveleasy-auth-test");
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        String email = "operator@expressbus.com";
        Set<UserRole> roles = Set.of(UserRole.ROLE_OPERATOR);

        String token = tokenProvider.generateToken(userId, email, operatorId, roles);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        UserPrincipal principal = tokenProvider.extractUserPrincipal(token);
        assertEquals(userId, principal.getUserId());
        assertEquals(email, principal.getEmail());
        assertEquals(operatorId, principal.getOperatorId());
        assertTrue(principal.getRoles().contains(UserRole.ROLE_OPERATOR));
    }

    @Test
    void shouldRejectMalformedOrTamperedToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.string"));
        assertFalse(tokenProvider.validateToken(""));
    }
}
