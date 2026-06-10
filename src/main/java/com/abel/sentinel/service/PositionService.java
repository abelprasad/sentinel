package com.abel.sentinel.service;

import com.abel.sentinel.dto.PositionDTO;
import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.AircraftEntityRepository;
import com.abel.sentinel.repository.AnomalyScoreRepository;
import com.abel.sentinel.repository.FlightEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PositionService {

    private final FlightEventRepository flightEventRepository;
    private final AircraftEntityRepository entityRepository;
    private final AnomalyScoreRepository anomalyScoreRepository;

    public PositionService(FlightEventRepository flightEventRepository,
                           AircraftEntityRepository entityRepository,
                           AnomalyScoreRepository anomalyScoreRepository) {
        this.flightEventRepository = flightEventRepository;
        this.entityRepository = entityRepository;
        this.anomalyScoreRepository = anomalyScoreRepository;
    }

    public List<PositionDTO> getCurrentPositions() {
        Instant since = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<FlightEvent> latest = flightEventRepository.findLatestPerEntitySince(since);

        return latest.stream()
                .map(event -> {
                    AircraftEntity entity = entityRepository.findById(event.getEntityId()).orElse(null);
                    if (entity == null) return null;

                    // only flag as anomalous if scored in the last 5 minutes
                    Instant recentThreshold = Instant.now().minus(5, ChronoUnit.MINUTES);
                    boolean anomalous = anomalyScoreRepository
                            .findByEntityOrderByFlaggedAtDesc(entity)
                            .stream()
                            .anyMatch(a -> a.getFlaggedAt().isAfter(recentThreshold));

                    return new PositionDTO(
                            entity.getId(),
                            entity.getCallsign(),
                            entity.getIcaoHex(),
                            event.getLat(),
                            event.getLon(),
                            event.getAltitude(),
                            event.getSpeed(),
                            event.getHeading(),
                            anomalous
                    );
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }
}