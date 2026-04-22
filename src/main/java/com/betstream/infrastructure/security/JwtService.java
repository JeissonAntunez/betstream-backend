package com.betstream.infrastructure.security;
import com.betstream.application.dto.AuthDtos;
import com.betstream.application.dto.UserResponse;
import com.betstream.domain.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${spring.security.jwt.secret}")
    private String secret;
    @Value("${spring.security.jwt.expiration:86400000}")
    private long expiration;
    @Value("${spring.security.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public AuthDtos.AuthResponse generateTokenPair(User user) {
        String accessToken = buildToken(user, expiration);
        String refreshToken = buildToken(user, refreshExpiration);
        return new AuthDtos.AuthResponse(
                accessToken, refreshToken, "Bearer", expiration / 1000,
                new UserResponse(user.getId(), user.getUsername(), user.getEmail(), BigDecimal.ZERO)
        );
    }
    private String buildToken(User user, long ttl) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(getKey())
                .compact();
    }
    public UUID extractUserIdFromHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Claims claims = Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }
    public Claims validateToken(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
    }
}
