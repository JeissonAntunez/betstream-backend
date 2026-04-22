package com.betstream.presentation.controller;

import com.betstream.application.dto.BetDtos;
import com.betstream.application.dto.EventDtos;
import com.betstream.domain.model.SportEvent;
import com.betstream.domain.repository.SportEventRepository;
import com.betstream.domain.service.BetService;
import com.betstream.domain.service.OddsEngineService;
import com.betstream.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

// ===========================
// EVENTS CONTROLLER
// ===========================
@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {

    private final SportEventRepository eventRepository;
    private final OddsEngineService oddsEngine;

    @GetMapping
    public Flux<SportEvent> getAllEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sport) {

        if (status != null && sport != null) {
            return eventRepository.findBySportAndStatus(
                    sport.toUpperCase(),
                    SportEvent.EventStatus.valueOf(status.toUpperCase()));
        }
        if ("LIVE".equalsIgnoreCase(status)) {
            return eventRepository.findAllLiveEvents();
        }
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<SportEvent> getEvent(@PathVariable UUID id) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found")));
    }

    @GetMapping("/live")
    public Flux<SportEvent> getLiveEvents() {
        return eventRepository.findAllLiveEvents();
    }

    @GetMapping("/upcoming")
    public Flux<SportEvent> getUpcomingEvents() {
        return eventRepository.findUpcomingNext24h();
    }

    /**
     * SSE endpoint: streams real-time odds updates to the client.
     * Client subscribes once and receives a continuous event stream.
     */
    @GetMapping(value = "/odds/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventDtos.OddsUpdateEvent>> streamOdds(
            @RequestParam(required = false) UUID eventId) {

        Flux<EventDtos.OddsUpdateEvent> stream = oddsEngine.getOddsStream();

        if (eventId != null) {
            stream = stream.filter(e -> e.eventId().equals(eventId));
        }

        return stream
                .map(odds -> ServerSentEvent.<EventDtos.OddsUpdateEvent>builder()
                        .id(UUID.randomUUID().toString())
                        .event("odds-update")
                        .data(odds)
                        .build())
                // SSE heartbeat every 15s to keep connection alive
                .mergeWith(Flux.interval(Duration.ofSeconds(15))
                        .map(i -> ServerSentEvent.<EventDtos.OddsUpdateEvent>builder()
                                .event("heartbeat")
                                .comment("keep-alive")
                                .build()));
    }

    /**
     * SSE endpoint: streams live score updates.
     */
    @GetMapping(value = "/scores/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventDtos.ScoreUpdateEvent>> streamScores() {
        return oddsEngine.getScoreStream()
                .map(score -> ServerSentEvent.<EventDtos.ScoreUpdateEvent>builder()
                        .id(UUID.randomUUID().toString())
                        .event("score-update")
                        .data(score)
                        .build());
    }
}

// ===========================
// BETS CONTROLLER
// ===========================
@Slf4j
@RestController
@RequestMapping("/api/v1/bets")
@RequiredArgsConstructor
class BetController {

    private final BetService betService;
    private final JwtService jwtService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BetDtos.BetResponse> placeBet(
            @Valid @RequestBody BetDtos.PlaceBetRequest request,
            @RequestHeader("Authorization") String authHeader) {

        UUID userId = jwtService.extractUserIdFromHeader(authHeader);
        return betService.placeBet(userId, request);
    }

    @GetMapping("/my")
    public Flux<BetDtos.BetResponse> getMyBets(
            @RequestHeader("Authorization") String authHeader) {

        UUID userId = jwtService.extractUserIdFromHeader(authHeader);
        return betService.getUserBets(userId);
    }
}
