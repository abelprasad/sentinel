package com.abel.sentinel.service;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.AnomalyScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AnomalyScoreService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyScoreService.class);

    private final AnomalyScoreRepository anomalyScoreRepository;
    private final BaselineService baselineService;
    private final LlmService llmService;

    public AnomalyScoreService(AnomalyScoreRepository anomalyScoreRepository,
                               BaselineService baselineService,
                               LlmService llmService) {
        this.anomalyScoreRepository = anomalyScoreRepository;
        this.baselineService = baselineService;
        this.llmService = llmService;
    }

    public static final double ANOMALY_THRESHOLD = 0.7;

    public Optional<AnomalyScore> score(AircraftEntity entity, FlightEvent event) {
        Optional<Baseline> baselineOpt = baselineService.getBaseline(entity);

        if (baselineOpt.isEmpty()) {
            return Optional.empty();
        }

        Baseline baseline = baselineOpt.get();

        log.info("Scoring event {} against baseline: alt={}, speed={}, heading={}",
                event.getId(), baseline.getAvgAltitude(), baseline.getAvgSpeed(), baseline.getAvgHeading());

        double altitudeDev  = deviation(event.getAltitude(), baseline.getAvgAltitude(), 10000.0);
        double speedDev     = deviation(event.getSpeed(), baseline.getAvgSpeed(), 200.0);
        double headingDev   = deviation(event.getHeading(), baseline.getAvgHeading(), 180.0);
        double latDev       = deviation(event.getLat(), baseline.getAvgLat(), 10.0);
        double lonDev       = deviation(event.getLon(), baseline.getAvgLon(), 10.0);

        double score = Math.max(Math.max(Math.max(altitudeDev, speedDev), headingDev), Math.max(latDev, lonDev));

        log.info("Anomaly score for event {}: {}", event.getId(), score);

        if (score < ANOMALY_THRESHOLD) {
            return Optional.empty();
        }

        // dedup -- skip if this entity was already flagged in the last 5 minutes
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        if (anomalyScoreRepository.existsByEntityAndFlaggedAtAfter(entity, fiveMinutesAgo)) {
            log.info("Skipping duplicate anomaly for entity {} -- already flagged within 5 minutes", entity.getId());
            return Optional.empty();
        }

        // try Groq first, fall back to rule-based
        String explanation = null;
        try {
            explanation = llmService.summarizeAnomaly(entity, event, baseline, score);
            if (explanation != null) {
                log.info("Groq explanation for event {}: {}", event.getId(), explanation);
            }
        } catch (Exception e) {
            log.warn("LLM call failed for event {}, using fallback: {}", event.getId(), e.getMessage());
        }
        if (explanation == null) {
            explanation = buildExplanation(altitudeDev, speedDev, headingDev, score);
        }

        AnomalyScore anomaly = new AnomalyScore();
        anomaly.setEntity(entity);
        anomaly.setEvent(event);
        anomaly.setScore(score);
        anomaly.setExplanation(explanation);
        anomaly.setFlaggedAt(Instant.now());

        return Optional.of(anomalyScoreRepository.save(anomaly));
    }

    public List<AnomalyScore> getAnomaliesForEntity(AircraftEntity entity) {
        return anomalyScoreRepository.findByEntityOrderByFlaggedAtDesc(entity);
    }

    public List<AnomalyScore> getAllAnomalies() {
        return anomalyScoreRepository.findAll();
    }

    public List<AnomalyScore> getHighSeverityAnomalies() {
        return anomalyScoreRepository.findByScoreGreaterThanOrderByFlaggedAtDesc(ANOMALY_THRESHOLD);
    }

    private double deviation(Double actual, Double baseline, double maxExpectedDelta) {
        if (actual == null || baseline == null) return 0.0;
        return Math.min(Math.abs(actual - baseline) / maxExpectedDelta, 1.0);
    }

    private String buildExplanation(double altDev, double speedDev, double headingDev, double score) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Anomaly score: %.2f. ", score));
        if (altDev > 0.5) sb.append("Significant altitude deviation. ");
        if (speedDev > 0.5) sb.append("Significant speed deviation. ");
        if (headingDev > 0.5) sb.append("Significant heading deviation. ");
        return sb.toString().trim();
    }
}