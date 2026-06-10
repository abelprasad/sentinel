package com.abel.sentinel.service;

import com.abel.sentinel.repository.AnomalyScoreRepository;
import com.abel.sentinel.repository.FlightEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PruneService {

    private final AnomalyScoreRepository anomalyScoreRepository;
    private final FlightEventRepository flightEventRepository;

    // Runs every day at 3:00 AM server time
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void pruneOldData() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);

        int anomaliesDeleted = anomalyScoreRepository.deleteByFlaggedAtBefore(cutoff);
        int eventsDeleted = flightEventRepository.deleteByTimestampBefore(cutoff);

        log.info("Prune complete -- {} anomalies, {} events older than 7 days removed", anomaliesDeleted, eventsDeleted);
    }
}