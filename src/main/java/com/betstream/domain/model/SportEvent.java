package com.betstream.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// =====================
// SPORT EVENT
// =====================
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sport_events")
public class SportEvent {
    @Id
    private UUID id;
    private String homeTeam;
    private String awayTeam;
    private String sport;        // FOOTBALL, BASKETBALL, TENNIS...
    private String league;
    private String country;
    private LocalDateTime startTime;
    private EventStatus status;  // SCHEDULED, LIVE, FINISHED, CANCELLED
    private Integer homeScore;
    private Integer awayScore;
    private String currentMinute; // "45+2", "HT", "FT"
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum EventStatus {
        SCHEDULED, LIVE, FINISHED, CANCELLED
    }
}

// =====================
// ODDS
// =====================
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("odds")
class Odds {
    @Id
    private UUID id;
    private UUID eventId;
    private String market;     // MATCH_WINNER, OVER_UNDER, BOTH_TEAMS_SCORE...
    private String selection;  // HOME, DRAW, AWAY, OVER_2_5, YES, NO...
    private BigDecimal value;
    private Boolean active;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
