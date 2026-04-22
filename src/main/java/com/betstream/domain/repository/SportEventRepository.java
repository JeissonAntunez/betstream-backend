package com.betstream.domain.repository;
import com.betstream.domain.model.SportEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface SportEventRepository extends R2dbcRepository<SportEvent, UUID> {
    Flux<SportEvent> findByStatus(SportEvent.EventStatus status);
    Flux<SportEvent> findBySportAndStatus(String sport, SportEvent.EventStatus status);
    @Query("SELECT * FROM sport_events WHERE status = 'LIVE' ORDER BY start_time ASC")
    Flux<SportEvent> findAllLiveEvents();
    @Query("SELECT * FROM sport_events WHERE status = 'SCHEDULED' AND start_time BETWEEN NOW() AND NOW() + INTERVAL '24 hours' ORDER BY start_time ASC")
    Flux<SportEvent> findUpcomingNext24h();
}
