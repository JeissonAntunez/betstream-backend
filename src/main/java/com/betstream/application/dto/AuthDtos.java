package com.betstream.application.dto;
import jakarta.validation.constraints.*;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 20) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password
    ) {}
    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            UserResponse user
    ) {}
    public record RefreshTokenRequest(@NotBlank String refreshToken) {}
}
