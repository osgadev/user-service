package com.ecommerce.userservice.security.config.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    private final Algorithm algorithm;

    public JwtUtil(@Value("${JWT_SECRET}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String create(String email, Long userId, String role, String jwtId, Instant expiresAt) {
        return JWT.create()
                .withSubject(email)
                .withIssuer("Users-MS")
                .withJWTId(jwtId)
                .withClaim("userId", userId)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        DecodedJWT decoded = JWT.require(algorithm)
                                .withIssuer("Users-MS")
                                .build()
                                .verify(token);

        if (decoded.getSubject() == null || decoded.getId() == null || decoded.getExpiresAt() == null) {

            throw new JWTVerificationException("JWT incompleto");
        }
        return decoded;
    }
}
