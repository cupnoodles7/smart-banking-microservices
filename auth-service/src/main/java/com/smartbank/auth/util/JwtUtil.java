package com.smartbank.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Signs and reads JWTs (jjwt 0.12 API).
 *
 * <p>The signing secret and lifetimes come from {@code security.jwt.*} — the same
 * secret the API Gateway uses to verify tokens locally, so tokens issued here are
 * accepted there without any call back to this service.
 *
 * <p>Access tokens carry {@code customerId}, {@code email} and {@code roles} claims
 * (subject = username); the Gateway forwards these downstream as trusted headers.
 */
@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${security.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ---- Extraction ----

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractCustomerId(String token) {
        return extractAllClaims(token).get("customerId", String.class);
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        // jjwt + Jackson deserializes the JSON array into an ArrayList, not a Set.
        if (roles instanceof Collection) {
            return new HashSet<>((Collection<String>) roles);
        }
        return Collections.emptySet();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ---- Generation ----

    public String generateAccessToken(String username, String customerId, String email, Set<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("customerId", customerId);
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put("type", "access");
        return buildToken(claims, username, accessTokenExpiryMs);
    }

    public String generateRefreshToken(String username, String customerId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("customerId", customerId);
        claims.put("type", "refresh");
        return buildToken(claims, username, refreshTokenExpiryMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ---- Validation ----

    /** Signature + expiry check only. */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token, String username) {
        return username.equals(extractUsername(token)) && !isTokenExpired(token);
    }

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }
}
