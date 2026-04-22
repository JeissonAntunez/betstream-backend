package com.betstream.domain.service;

import com.betstream.application.dto.BetDtos;
import com.betstream.domain.model.Bet;
import com.betstream.domain.model.SportEvent;
import com.betstream.domain.repository.BetRepository;
import com.betstream.domain.repository.SportEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BetService {

    private final BetRepository betRepository;
    private final SportEventRepository eventRepository;
    private final OddsEngineService oddsEngine;
    private final WalletService walletService;

    /**
     * Place a bet with full validation chain (reactive pipeline).
     */
    @Transactional
    public Mono<BetDtos.BetResponse> placeBet(UUID userId, BetDtos.PlaceBetRequest request) {
        return eventRepository.findById(request.eventId())
                // Validate event exists
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found: " + request.eventId())))
                // Validate event accepts bets
                .flatMap(event -> validateEventAcceptsBets(event, request))
                // Validate wallet balance
                .flatMap(event -> walletService.deductBalance(userId, request.stake())
                        .thenReturn(event))
                // Build and persist bet
                .flatMap(event -> {
                    BigDecimal currentOdds = oddsEngine.getCurrentOdds(
                            request.eventId(), request.market(), request.selection());

                    Bet bet = Bet.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .eventId(request.eventId())
                            .market(request.market())
                            .selection(request.selection())
                            .oddsValue(currentOdds)
                            .stake(request.stake())
                            .potentialWin(request.stake().multiply(currentOdds))
                            .status(Bet.BetStatus.PENDING)
                            .placedAt(LocalDateTime.now())
                            .build();

                    return betRepository.save(bet)
                            .map(saved -> mapToBetResponse(saved, event));
                })
                .doOnSuccess(b -> log.info("Bet placed: {} by user {}", b.id(), userId))
                .doOnError(e -> log.error("Bet placement failed for user {}: {}", userId, e.getMessage()));
    }

    /**
     * Get all bets for a user (pageable-ready Flux).
     */
    public Flux<BetDtos.BetResponse> getUserBets(UUID userId) {
        return betRepository.findByUserIdOrderByPlacedAtDesc(userId)
                .flatMap(bet -> eventRepository.findById(bet.getEventId())
                        .map(event -> mapToBetResponse(bet, event)));
    }

    /**
     * Get pending bets for a specific event.
     */
    public Flux<Bet> getPendingBetsForEvent(UUID eventId) {
        return betRepository.findByEventIdAndStatus(eventId, Bet.BetStatus.PENDING);
    }

    /**
     * Settle all bets for a finished event.
     * Called by event settlement service.
     */
    @Transactional
    public Flux<Bet> settleEventBets(UUID eventId, String winningSelection, String market) {
        return betRepository.findByEventIdAndStatus(eventId, Bet.BetStatus.PENDING)
                .filter(bet -> bet.getMarket().equals(market))
                .flatMap(bet -> {
                    boolean won = bet.getSelection().equals(winningSelection);
                    bet.setStatus(won ? Bet.BetStatus.WON : Bet.BetStatus.LOST);
                    bet.setSettledAt(LocalDateTime.now());

                    if (won) {
                        bet.setSettledAmount(bet.getPotentialWin());
                        return walletService.creditBalance(bet.getUserId(), bet.getPotentialWin())
                                .then(betRepository.save(bet));
                    }
                    return betRepository.save(bet);
                });
    }

    private Mono<SportEvent> validateEventAcceptsBets(SportEvent event, BetDtos.PlaceBetRequest request) {
        if (event.getStatus() == SportEvent.EventStatus.FINISHED ||
                event.getStatus() == SportEvent.EventStatus.CANCELLED) {
            return Mono.error(new IllegalStateException("Event is not accepting bets: " + event.getStatus()));
        }
        return Mono.just(event);
    }

    private BetDtos.BetResponse mapToBetResponse(Bet bet, SportEvent event) {
        return new BetDtos.BetResponse(
                bet.getId(),
                bet.getEventId(),
                event.getHomeTeam(),
                event.getAwayTeam(),
                bet.getMarket(),
                bet.getSelection(),
                bet.getOddsValue(),
                bet.getStake(),
                bet.getPotentialWin(),
                bet.getStatus(),
                bet.getSettledAmount(),
                bet.getPlacedAt(),
                bet.getSettledAt()
        );
    }
}
