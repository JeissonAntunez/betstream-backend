package com.betstream.infrastructure.security;

import com.betstream.application.dto.AuthDtos;
import com.betstream.domain.model.User;
import com.betstream.domain.repository.UserRepository;
import com.betstream.domain.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return userRepository.existsByEmail(req.email())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new IllegalArgumentException("Email already registered"));
                    return userRepository.existsByUsername(req.username());
                })
                .flatMap(usernameExists -> {
                    if (usernameExists) return Mono.error(new IllegalArgumentException("Username taken"));
                    User user = User.builder()
                            .id(UUID.randomUUID())
                            .username(req.username())
                            .email(req.email())
                            .passwordHash(passwordEncoder.encode(req.password()))
                            .role("USER")
                            .active(true)
                            .emailVerified(false)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(user);
                })
                .flatMap(user -> walletService.initBalance(user.getId(), BigDecimal.valueOf(100.00))
                        .thenReturn(user))
                .map(user -> jwtService.generateTokenPair(user));
    }

    @PostMapping("/login")
    public Mono<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return userRepository.findByEmail(req.email())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid credentials")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
                        return Mono.error(new IllegalArgumentException("Invalid credentials"));
                    }
                    if (!user.getActive()) {
                        return Mono.error(new IllegalStateException("Account is disabled"));
                    }
                    return Mono.just(jwtService.generateTokenPair(user));
                });
    }
}
