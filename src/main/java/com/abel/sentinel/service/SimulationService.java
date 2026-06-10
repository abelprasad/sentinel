package com.abel.sentinel.service;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.AircraftEntityRepository;
import com.abel.sentinel.repository.FlightEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final AircraftEntityRepository entityRepository;
    private final FlightEventRepository flightEventRepository;
    private final BaselineService baselineService;
    private final AnomalyScoreService anomalyScoreService;

    private final Random random = new Random();

    /**
     * Quick fire -- picks a random entity that has a baseline and injects
     * a synthetic event guaranteed to score 1.0 (2x max deviation on altitude + speed).
     */
    public Optional<AnomalyScore> quickFire() {
        List<AircraftEntity> all = entityRepository.findAll();

        // shuffle and find first entity that has a baseline
        java.util.Collections.shuffle(all);
        for (AircraftEntity entity : all) {
            Optional<Baseline> baselineOpt = baselineService.getBaseline(entity);
            if (baselineOpt.isEmpty()) continue;

            Baseline baseline = baselineOpt.get();

            // spike altitude and speed well beyond max delta to guarantee score 1.0
            double altitude = baseline.getAvgAltitude() + 12000.0;
            double speed    = baseline.getAvgSpeed()    + 250.0;
            double heading  = (baseline.getAvgHeading() + 180.0) % 360.0;

            return inject(entity, altitude, speed, heading, baseline.getAvgLat(), baseline.getAvgLon());
        }

        log.warn("Quick fire: no entities with baselines found");
        return Optional.empty();
    }

    /**
     * Custom injection -- caller supplies all values explicitly.
     */
    public Optional<AnomalyScore> custom(Long entityId, Double altitude, Double speed,
                                         Double heading, Double lat, Double lon) {
        Optional<AircraftEntity> entityOpt = entityRepository.findById(entityId);
        if (entityOpt.isEmpty()) {
            log.warn("Simulation: entity {} not found", entityId);
            return Optional.empty();
        }

        AircraftEntity entity = entityOpt.get();
        Optional<Baseline> baselineOpt = baselineService.getBaseline(entity);
        if (baselineOpt.isEmpty()) {
            log.warn("Simulation: no baseline for entity {}", entityId);
            return Optional.empty();
        }

        Baseline baseline = baselineOpt.get();

        // fall back to baseline lat/lon if caller didn't supply them
        double finalLat = lat != null ? lat : baseline.getAvgLat();
        double finalLon = lon != null ? lon : baseline.getAvgLon();

        return inject(entity, altitude, speed, heading, finalLat, finalLon);
    }

    private Optional<AnomalyScore> inject(AircraftEntity entity, double altitude, double speed,
                                          double heading, double lat, double lon) {
        FlightEvent event = new FlightEvent();
        event.setEntityId(entity.getId());
        event.setTimestamp(Instant.now());
        event.setAltitude(altitude);
        event.setSpeed(speed);
        event.setHeading(heading);
        event.setLat(lat);
        event.setLon(lon);

        FlightEvent saved = flightEventRepository.save(event);
        log.info("Simulation: injected synthetic event {} for entity {} ({})",
                saved.getId(), entity.getId(), entity.getCallsign());

        return anomalyScoreService.score(entity, saved);
    }
}