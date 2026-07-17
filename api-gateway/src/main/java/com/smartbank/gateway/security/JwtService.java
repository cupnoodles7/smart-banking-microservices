package com.smartbank.gateway.security;

import com.smartbank.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Verifies and parses HMAC-signed JWTs at the edge.
 *
 * <p>Only signature/expiry validation and claim extraction happen here — token
 * issuance and refresh are the Auth Service's responsibility.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses and verifies the token, returning its claims.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired,
     *         or the signature does not verify.
     */
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }
}
