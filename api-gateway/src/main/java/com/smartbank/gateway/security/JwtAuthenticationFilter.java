package com.smartbank.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

import com.smartbank.gateway.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

//Global gateway filter that enforces JWT presence and validity on every route except the configured open paths 
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    // Headers downstream services trust as gateway-asserted identity, plus the
    // service-to-service key. A client must never supply these, so they are
    // stripped from every inbound request before the gateway sets its own.
    private static final List<String> TRUSTED_HEADERS = List.of(
            "X-Auth-Username", "X-Customer-Id", "X-User-Email", "X-Auth-Roles", "X-Internal-Api-Key");

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

        // Strip any client-supplied trusted headers up front — on every route,
        // open or not — so they can only ever carry values the gateway sets.
        ServerHttpRequest scrubbed = request.mutate()
                .headers(h -> TRUSTED_HEADERS.forEach(h::remove))
                .build();

        if (isOpen(path)) {
            return chain.filter(exchange.mutate().request(scrubbed).build());
        }

        String authHeader = scrubbed.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, path, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.parse(token);
            // Only access tokens are accepted at the edge
            if (!"access".equals(claims.get("type"))) {
                log.warn("Rejected request to {}: token is not an access token", path);
                return unauthorized(exchange, path, "Access token required");
            }
            ServerHttpRequest mutated = scrubbed.mutate()
                    // Identity forwarded to downstream services as trusted headers
                    // subject = username; customerId/email/roles are explicit claims
                    .header("X-Auth-Username", String.valueOf(claims.getSubject()))
                    .header("X-Customer-Id", String.valueOf(claims.get("customerId")))
                    .header("X-User-Email", String.valueOf(claims.get("email")))
                    .header("X-Auth-Roles", rolesHeader(claims))
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

   
    private String rolesHeader(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return "";
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
        // this runs before routing so unauthenticated requests never reach downstream services
        return -1;
    }
}
