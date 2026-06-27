package com.labplatform.equipment.security;
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
                setAuthentication(gatewayUsername, gatewayRole, request);
                filterChain.doFilter(request, response);
                return;
            }
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());
                if (jwtUtil.validateToken(token)) {
                    setAuthentication(jwtUtil.extractUsername(token), jwtUtil.extractRole(token), request);
                }
            }
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
    private void setAuthentication(String username, String role, HttpServletRequest request) {
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(normalizedRole)));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
