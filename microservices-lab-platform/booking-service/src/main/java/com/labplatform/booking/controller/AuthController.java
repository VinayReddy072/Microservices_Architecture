package com.labplatform.booking.controller;
import com.labplatform.booking.dto.LoginRequest;
import com.labplatform.booking.dto.TokenResponse;
import com.labplatform.booking.security.JwtUtil;
import com.labplatform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("Authentication attempt for username={}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");
        String token = jwtUtil.generateToken(userDetails.getUsername(), role);
        long expiresIn = jwtUtil.getExpirationMs() / 1000;
        log.info("Authentication successful for username={} role={}", request.getUsername(), role);
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(userDetails.getUsername())
                .role(role)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", tokenResponse));
    }
}
