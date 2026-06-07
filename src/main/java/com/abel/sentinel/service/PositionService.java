package com.abel.sentinel.service;

import com.abel.sentinel.dto.PositionDTO;
import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.AircraftEntityRepository;
import com.abel.sentinel.repository.AnomalyScoreRepository;
import com.abel.sentinel.repository.FlightEventRepository;
import org.springframework.stereotype.Service;

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
        List<FlightEvent> latest = flightEventRepository.findLatestPerEntity();

        return latest.stream()
                .map(event -> {
                    AircraftEntity entity = entityRepository.findById(event.getEntityId()).orElse(null);
                    if (entity == null) return null;

                    boolean anomalous = !anomalyScoreRepository
                            .findByEntityOrderByFlaggedAtDesc(entity).isEmpty();

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