package com.betstream.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("bets")
public class Bet {
    @Id
    private UUID id;
    private UUID userId;
    private UUID eventId;
    private String market;
    private String selection;
    private BigDecimal oddsValue;      // Odds snapshot at bet placement
    private BigDecimal stake;          // Amount wagered
    private BigDecimal potentialWin;   // stake * oddsValue
    private BetStatus status;
    private BigDecimal settledAmount;  // Actual payout if won
    @CreatedDate
    private LocalDateTime placedAt;
    private LocalDateTime settledAt;

    public enum BetStatus {
        PENDING, WON, LOST, CANCELLED, CASHED_OUT
    }

    // Domain method
    public BigDecimal calculatePotentialWin() {
        return this.stake.multiply(this.oddsValue);
    }
}
