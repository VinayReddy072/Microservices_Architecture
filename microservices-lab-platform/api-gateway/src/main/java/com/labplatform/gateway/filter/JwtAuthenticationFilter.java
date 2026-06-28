package com.labplatform.gateway.filter;
import com.labplatform.gateway.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class JwtAuthenticationFilter implements GlobalFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USERNAME = "X-Auth-Username";
    private static final String HEADER_ROLE = "X-Auth-Role";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator",
            "/fallback",
            "/swagger-ui",
            "/v3/api-docs"
    );
    private final JwtUtil jwtUtil;
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        log.debug("Gateway processing request: {} {}", request.getMethod(), path);
        if (isPublicPath(path)) {
            log.debug("Public path — skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return buildUnauthorizedResponse(exchange, "Missing or malformed Authorization header");
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT for path: {}", path);
            return buildUnauthorizedResponse(exchange, "Invalid or expired JWT token");
        }
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);
        log.debug("JWT valid. Forwarding request from user={} role={} to path={}", username, role, path);
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HEADER_USERNAME, username)
                .header(HEADER_ROLE, role)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    private Mono<Void> buildUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        byte[] bytes = Objects.requireNonNull(body.getBytes(StandardCharsets.UTF_8));
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Objects.requireNonNull(Flux.just(buffer)));
    }
}
