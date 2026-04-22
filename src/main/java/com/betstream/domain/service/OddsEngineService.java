package com.betstream.domain.service;

import com.betstream.application.dto.EventDtos;
import com.betstream.domain.model.SportEvent;
import com.betstream.domain.repository.SportEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OddsEngine - Core reactive service.
 * Maintains a hot Flux<OddsUpdateEvent> that emits odds changes
 * for all live events. Controllers subscribe via SSE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OddsEngineService {

    private final SportEventRepository eventRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final Random random = new Random();

    @Value("${betstream.odds.fluctuation-range:0.15}")
    private double fluctuationRange;

    // In-memory current odds (eventId+market+selection -> value)
    private final Map<String, BigDecimal> currentOdds = new ConcurrentHashMap<>();

    // Multicast sink: all SSE subscribers share the same hot stream
    private final Sinks.Many<EventDtos.OddsUpdateEvent> oddsSink =
            Sinks.many().multicast().onBackpressureBuffer();

    private final Sinks.Many<EventDtos.ScoreUpdateEvent> scoreSink =
            Sinks.many().multicast().onBackpressureBuffer();

    /**
     * Returns the hot Flux for SSE streaming to clients.
     */
    public Flux<EventDtos.OddsUpdateEvent> getOddsStream() {
        return oddsSink.asFlux();
    }

    public Flux<EventDtos.ScoreUpdateEvent> getScoreStream() {
        return scoreSink.asFlux();
    }

    /**
     * Scheduled task: randomly fluctuate odds for all live events.
     * Runs every 3 seconds.
     */
    @Scheduled(fixedRateString = "${betstream.odds.update-interval-ms:3000}")
    public void updateLiveOdds() {
        eventRepository.findAllLiveEvents()
                .flatMap(event -> {
                    var updates = generateOddsFluctuations(event);
                    return Flux.fromIterable(updates);
                })
                .doOnNext(update -> {
                    // Cache new value in Redis for fast reads
                    String key = buildOddsKey(update.eventId(), update.market(), update.selection());
                    redisTemplate.opsForValue()
                            .set(key, update.newValue().toPlainString())
                            .subscribe();

                    // Emit to all SSE subscribers
                    oddsSink.tryEmitNext(update);
                })
                .doOnError(e -> log.error("Error updating live odds: {}", e.getMessage()))
                .subscribe();
    }

    /**
     * Generate realistic odds fluctuations for an event.
     */
    private java.util.List<EventDtos.OddsUpdateEvent> generateOddsFluctuations(SportEvent event) {
        var updates = new java.util.ArrayList<EventDtos.OddsUpdateEvent>();
        var markets = getDefaultMarkets(event);

        for (var entry : markets.entrySet()) {
            String market = entry.getKey();
            for (var selEntry : entry.getValue().entrySet()) {
                String selection = selEntry.getKey();
                BigDecimal baseOdds = selEntry.getValue();
                String cacheKey = buildOddsKey(event.getId(), market, selection);

                BigDecimal currentValue = currentOdds.getOrDefault(cacheKey, baseOdds);
                BigDecimal newValue = fluctuateOdds(currentValue);
                currentOdds.put(cacheKey, newValue);

                updates.add(new EventDtos.OddsUpdateEvent(
                        event.getId(),
                        market,
                        selection,
                        newValue,
                        currentValue,
                        LocalDateTime.now()
                ));
            }
        }
        return updates;
    }

    private BigDecimal fluctuateOdds(BigDecimal current) {
        double change = (random.nextDouble() * 2 - 1) * fluctuationRange;
        BigDecimal newVal = current.multiply(BigDecimal.valueOf(1 + change));
        // Clamp: min 1.01, max 50.0
        newVal = newVal.max(BigDecimal.valueOf(1.01)).min(BigDecimal.valueOf(50.0));
        return newVal.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Map<String, BigDecimal>> getDefaultMarkets(SportEvent event) {
        // 1X2 Market
        Map<String, BigDecimal> matchWinner = Map.of(
                "HOME",  BigDecimal.valueOf(1.85 + random.nextDouble()),
                "DRAW",  BigDecimal.valueOf(3.20 + random.nextDouble()),
                "AWAY",  BigDecimal.valueOf(4.10 + random.nextDouble())
        );
        // Over/Under 2.5
        Map<String, BigDecimal> overUnder = Map.of(
                "OVER_2_5",  BigDecimal.valueOf(1.75 + random.nextDouble() * 0.5),
                "UNDER_2_5", BigDecimal.valueOf(1.95 + random.nextDouble() * 0.5)
        );
        return Map.of(
                "MATCH_WINNER", matchWinner,
                "OVER_UNDER_2_5", overUnder
        );
    }

    public BigDecimal getCurrentOdds(UUID eventId, String market, String selection) {
        String key = buildOddsKey(eventId, market, selection);
        return currentOdds.getOrDefault(key, BigDecimal.valueOf(1.0));
    }

    public void emitScoreUpdate(EventDtos.ScoreUpdateEvent event) {
        scoreSink.tryEmitNext(event);
    }

    private String buildOddsKey(UUID eventId, String market, String selection) {
        return "odds:" + eventId + ":" + market + ":" + selection;
    }
}
