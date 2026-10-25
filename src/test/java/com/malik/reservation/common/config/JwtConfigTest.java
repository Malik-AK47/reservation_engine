
package com.malik.reservation.common.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

class JwtConfigTest {

    private JwtConfig jwtConfig;
    private SecretKey secretKey;
    private JwtEncoder encoder;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();

        secretKey = jwtConfig.jwtSecretKey("test-secret-key-at-least-32-bytes-long");

        encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Test
    void shouldAcceptValidToken() {
        var decoder = jwtConfig.jwtDecoder(secretKey);
        String token = createToken(
                UUID.randomUUID().toString(),
                "USER",
                Instant.now(),
                Instant.now().plusSeconds(900),
                true
        );

        assertDoesNotThrow(() -> decoder.decode(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        var decoder = jwtConfig.jwtDecoder(secretKey);
        Instant now = Instant.now();

        String token = createToken(
                UUID.randomUUID().toString(),
                "USER",
                now.minusSeconds(600),
                now.minusSeconds(300),
                true
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void shouldRejectTokenWithInvalidRole() {
        var decoder = jwtConfig.jwtDecoder(secretKey);
        Instant now = Instant.now();

        String token = createToken(
                UUID.randomUUID().toString(),
                "SUPERUSER",
                now,
                now.plusSeconds(900),
                true
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void shouldRejectTokenMissingRequiredTimestamps() {
        var decoder = jwtConfig.jwtDecoder(secretKey);

        String token = createToken(
                UUID.randomUUID().toString(),
                "USER",
                null,
                null,
                false
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    private String createToken(
            String subject,
            String role,
            Instant issuedAt,
            Instant expiresAt,
            boolean includeTimestamps) {

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(subject)
                .claim("role", role);

        if (includeTimestamps) {
            claims.issuedAt(issuedAt);
            claims.expiresAt(expiresAt);
        }

        return encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }
}
