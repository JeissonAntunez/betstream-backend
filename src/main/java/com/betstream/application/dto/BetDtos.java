package com.betstream.application.dto;
import com.betstream.domain.model.Bet;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BetDtos {
    public record PlaceBetRequest(
            @NotNull UUID eventId,
            @NotBlank String market,
            @NotBlank String selection,
            @NotNull @DecimalMin("0.50") BigDecimal stake
    ) {}
    public record BetResponse(
            UUID id, UUID eventId, String homeTeam, String awayTeam,
            String market, String selection, BigDecimal oddsValue,
            BigDecimal stake, BigDecimal potentialWin, Bet.BetStatus status,
            BigDecimal settledAmount, LocalDateTime placedAt, LocalDateTime settledAt
    ) {}
}
