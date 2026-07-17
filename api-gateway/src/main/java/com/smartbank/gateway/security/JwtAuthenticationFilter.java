package com.smartbank.gateway.security;

import com.smartbank.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Global gateway filter that enforces JWT presence and validity on every route
 * except the configured open paths (PRD sec 6.8).
 *
 * <p>On success it forwards identity claims to the downstream service as trusted
 * headers ({@code X-Customer-Id}, {@code X-User-Email}) so services need not
 * re-parse the token. On failure it short-circuits with HTTP 401 in the standard
 * error shape (PRD sec 6.9).
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JwtProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtService jwtService, JwtProperties properties) {
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isOpen(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, path, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.parse(token);
            ServerHttpRequest mutated = request.mutate()
                    .header("X-Customer-Id", String.valueOf(claims.get("customerId")))
                    .header("X-User-Email", claims.getSubject())
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected request to {}: {}", path, ex.getMessage());
            return unauthorized(exchange, path, "Invalid or expired token");
        }
    }

    private boolean isOpen(String path) {
        return properties.getOpenPaths().stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String path, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"%s","path":"%s"}"""
                .formatted(java.time.OffsetDateTime.now(), message, path);

        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run before routing so unauthenticated requests never reach downstream services.
        return -1;
    }
}
