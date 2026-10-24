package com.ecommerce.userservice.security.config.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private final String secret = "test-only-secret-at-least-32-bytes-long";
    private final JwtUtil util = new JwtUtil(secret);

    @Test void tokenIncluyeIdentificadorYExpiracion() {
        Instant exp = Instant.now().plusSeconds(1800).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var token = util.verify(util.create("a@example.com", "id", exp));
        assertEquals("a@example.com", token.getSubject());
        assertEquals("id", token.getId());
        assertEquals(exp, token.getExpiresAtAsInstant());
    }

    @Test void rechazaTokenVencidoAlteradoOIncompleto() {
        assertThrows(JWTVerificationException.class, () -> util.verify("invalido"));
        assertThrows(JWTVerificationException.class, () ->
                util.verify(util.create("a@example.com", "id", Instant.now().minusSeconds(10))));
        String incomplete = JWT.create().withIssuer("Users-MS").sign(Algorithm.HMAC256(secret));
        assertThrows(JWTVerificationException.class, () -> util.verify(incomplete));
        String wrongIssuer = JWT.create().withIssuer("otro").sign(Algorithm.HMAC256(secret));
        assertThrows(JWTVerificationException.class, () -> util.verify(wrongIssuer));
    }
}
