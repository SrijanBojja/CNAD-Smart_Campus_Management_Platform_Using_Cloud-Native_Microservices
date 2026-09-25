package com.smartcampus.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(testSecret, testExpirationMs);
    }

    @Test
    @DisplayName("Should initialize successfully with valid 256-bit+ Base64 secret")
    void testValidBase64SecretWorks() {
        JwtTokenProvider provider = new JwtTokenProvider(testSecret, 3600000);
        assertNotNull(provider);
        assertEquals(3600000, provider.getExpirationMs());
    }

    @Test
    @DisplayName("Should initialize successfully with valid 32+ byte plain string secret")
    void testValidPlainUtf8SecretWorks() {
        String plainSecret = "this-is-a-secure-plain-text-key-with-over-32-bytes!";
        JwtTokenProvider provider = new JwtTokenProvider(plainSecret, 3600000);
        assertNotNull(provider);

        String token = provider.generateToken(10L, "testuser", "test@test.com", List.of("STUDENT"));
        assertTrue(provider.validateToken(token));
        assertEquals("testuser", provider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Should reject weak JWT secrets (< 32 bytes / 256 bits)")
    void testWeakSecretRejected() {
        // Short plain text secret
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider("short-weak-secret", 3600000));
        assertTrue(ex1.getMessage().contains("too weak for HS256"));

        // 31-byte secret (1 byte under required 32 bytes)
        String secret31Bytes = "1234567890123456789012345678901";
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider(secret31Bytes, 3600000));
        assertTrue(ex2.getMessage().contains("too weak for HS256"));
    }

    @Test
    @DisplayName("Should reject null or blank JWT secrets")
    void testNullOrBlankSecretRejected() {
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider(null, 3600000));
        assertTrue(exNull.getMessage().contains("must not be null or blank"));

        IllegalArgumentException exEmpty = assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider("", 3600000));
        assertTrue(exEmpty.getMessage().contains("must not be null or blank"));

        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider("   ", 3600000));
        assertTrue(exBlank.getMessage().contains("must not be null or blank"));
    }

    @Test
    @DisplayName("Should reject zero or negative expiration time")
    void testInvalidExpirationRejected() {
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider(testSecret, 0));
        assertTrue(exZero.getMessage().contains("expiration time must be greater than 0"));

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider(testSecret, -1000));
        assertTrue(exNeg.getMessage().contains("expiration time must be greater than 0"));
    }

    @Test
    @DisplayName("Should generate valid JWT token with user claims and roles")
    void testGenerateAndValidateToken() {
        Long userId = 1L;
        String username = "srijan";
        String email = "srijan@example.com";
        List<String> roles = List.of("STUDENT");

        String token = jwtTokenProvider.generateToken(userId, username, email, roles);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));

        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);
        assertEquals(1, extractedRoles.size());
        assertEquals("ROLE_STUDENT", extractedRoles.get(0));
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidpayload.invalidsignature";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));

        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken("malformed.token.value"));
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testExpiredToken() {
        // Create token provider with 1ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(testSecret, 1);
        String token = shortLivedProvider.generateToken(2L, "expiredUser", "exp@example.com", List.of("STUDENT"));

        // Sleep briefly to ensure expiration
        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {}

        assertFalse(shortLivedProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should properly extract multiple roles from JWT claims")
    void testMultipleRolesExtraction() {
        Long userId = 5L;
        String username = "adminuser";
        String email = "admin@campus.edu";
        List<String> roles = List.of("ADMIN", "FACULTY");

        String token = jwtTokenProvider.generateToken(userId, username, email, roles);
        assertTrue(jwtTokenProvider.validateToken(token));

        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);
        assertTrue(extractedRoles.contains("ROLE_ADMIN"));
        assertTrue(extractedRoles.contains("ROLE_FACULTY"));
    }
}
