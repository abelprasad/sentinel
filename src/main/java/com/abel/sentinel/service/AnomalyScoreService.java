package com.abel.sentinel.service;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.AnomalyScoreRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AnomalyScoreService {

    private final AnomalyScoreRepository anomalyScoreRepository;
    private final BaselineService baselineService;

    public AnomalyScoreService(AnomalyScoreRepository anomalyScoreRepository, BaselineService baselineService) {
        this.anomalyScoreRepository = anomalyScoreRepository;
        this.baselineService = baselineService;
    }

    public static final double ANOMALY_THRESHOLD = 0.7;

    public Optional<AnomalyScore> score(AircraftEntity entity, FlightEvent event) {
        Optional<Baseline> baselineOpt = baselineService.getBaseline(entity);

        if (baselineOpt.isEmpty()) {
            return Optional.empty();
        }

        Baseline baseline = baselineOpt.get();

        double altitudeDev  = deviation(event.getAltitude(), baseline.getAvgAltitude(), 10000.0);
        double speedDev     = deviation(event.getSpeed(), baseline.getAvgSpeed(), 200.0);
        double headingDev   = deviation(event.getHeading(), baseline.getAvgHeading(), 180.0);
        double latDev       = deviation(event.getLat(), baseline.getAvgLat(), 10.0);
        double lonDev       = deviation(event.getLon(), baseline.getAvgLon(), 10.0);

        double score = (altitudeDev + speedDev + headingDev + latDev + lonDev) / 5.0;
        score = Math.min(score, 1.0);

        if (score < ANOMALY_THRESHOLD) {
            return Optional.empty();
        }

        AnomalyScore anomaly = new AnomalyScore();
        anomaly.setEntity(entity);
        anomaly.setEvent(event);
        anomaly.setScore(score);
        anomaly.setExplanation(buildExplanation(altitudeDev, speedDev, headingDev, score));
        anomaly.setFlaggedAt(Instant.now());

        return Optional.of(anomalyScoreRepository.save(anomaly));
    }

    public List<AnomalyScore> getAnomaliesForEntity(AircraftEntity entity) {
        return anomalyScoreRepository.findByEntityOrderByFlaggedAtDesc(entity);
    }

    public List<AnomalyScore> getHighSeverityAnomalies() {
        return anomalyScoreRepository.findByEntityAndScoreGreaterThanOrderByFlaggedAtDesc(null, ANOMALY_THRESHOLD);
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