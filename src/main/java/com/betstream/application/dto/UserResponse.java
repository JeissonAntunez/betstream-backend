package com.betstream.application.dto;
import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(UUID id, String username, String email, BigDecimal balance) {}
