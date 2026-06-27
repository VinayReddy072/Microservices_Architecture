package com.labplatform.booking.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USERNAME = "X-Auth-Username";
    private static final String HEADER_ROLE = "X-Auth-Role";
    private final JwtUtil jwtUtil;
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String gatewayUsername = request.getHeader(HEADER_USERNAME);
            String gatewayRole = request.getHeader(HEADER_ROLE);
            if (StringUtils.hasText(gatewayUsername) && StringUtils.hasText(gatewayRole)) {
                log.debug("Authenticating via gateway headers: user={} role={}", gatewayUsername, gatewayRole);
                setAuthentication(gatewayUsername, gatewayRole, request);
                filterChain.doFilter(request, response);
                return;
            }
            String token = extractBearerToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                log.debug("Authenticating via JWT token: user={} role={}", username, role);
                setAuthentication(username, role, request);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
    private void setAuthentication(String username, String role, HttpServletRequest request) {
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(normalizedRole));
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
