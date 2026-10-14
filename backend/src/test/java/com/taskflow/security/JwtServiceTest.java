package com.taskflow.security;

import com.taskflow.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // Valid Base64 256-bit key
    private static final String SECRET_BASE64 = "dGFza2Zsb3ctc3VwZXItc2VjcmV0LWtleS0yNTYtYml0cy1sb25nLTIwMjY=";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_BASE64, EXPIRATION_MS);
    }

    @Test
    void generateToken_ShouldReturnValidJwt() {
        User user = new User();
        user.setEmail("alex@example.com");
        user.setName("Alex");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("alex@example.com", jwtService.extractEmail(token));
    }

    @Test
    void isTokenValid_ShouldReturnFalseForTamperedToken() {
        User user = new User();
        user.setEmail("alex@example.com");
        String token = jwtService.generateToken(user);

        String tampered = token + "xyz";

        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_ShouldReturnFalseForInvalidString() {
        assertFalse(jwtService.isTokenValid("not.a.valid.jwt"));
    }
}
