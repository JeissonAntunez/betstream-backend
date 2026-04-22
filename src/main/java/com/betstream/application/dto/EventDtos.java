package com.betstream.application.dto;
import com.betstream.domain.model.SportEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EventDtos {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SportEventResponse(
            UUID id, String homeTeam, String awayTeam, String sport,
            String league, String country, LocalDateTime startTime,
            SportEvent.EventStatus status, Integer homeScore, Integer awayScore,
            String currentMinute, List<OddsResponse> markets
    ) {}
    public record OddsResponse(UUID id, String market, String selection, BigDecimal value, Boolean active) {}
    public record OddsUpdateEvent(UUID eventId, String market, String selection,
            BigDecimal newValue, BigDecimal oldValue, LocalDateTime timestamp) {}
    public record ScoreUpdateEvent(UUID eventId, Integer homeScore, Integer awayScore,
            String currentMinute, SportEvent.EventStatus status, LocalDateTime timestamp) {}
}
