package com.betstream.domain.repository;
import com.betstream.domain.model.Bet;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface BetRepository extends R2dbcRepository<Bet, UUID> {
    Flux<Bet> findByUserIdOrderByPlacedAtDesc(UUID userId);
    Flux<Bet> findByUserIdAndStatus(UUID userId, Bet.BetStatus status);
    Flux<Bet> findByEventIdAndStatus(UUID eventId, Bet.BetStatus status);
    @Query("SELECT SUM(stake) FROM bets WHERE user_id = :userId AND status = 'PENDING'")
    Mono<BigDecimal> sumPendingStakeByUser(UUID userId);
}
