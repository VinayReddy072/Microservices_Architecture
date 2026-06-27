package com.labplatform.booking.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private String username;
    private String role;
}
