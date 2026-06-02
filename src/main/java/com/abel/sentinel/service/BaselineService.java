package com.abel.sentinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.BaselineRepository;
import com.abel.sentinel.repository.FlightEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BaselineService {

    private static final Logger log = LoggerFactory.getLogger(BaselineService.class);

    private final BaselineRepository baselineRepository;
    private final FlightEventRepository flightEventRepository;

    public BaselineService(BaselineRepository baselineRepository, FlightEventRepository flightEventRepository) {
        this.baselineRepository = baselineRepository;
        this.flightEventRepository = flightEventRepository;
    }

    public static final int MINIMUM_EVENTS = 3;

    public Optional<Baseline> calculate(AircraftEntity entity) {
        List<FlightEvent> events = flightEventRepository.findByEntityId(entity.getId());

        if (events.size() < MINIMUM_EVENTS) {
            return Optional.empty();
        }

        log.info("Calculating baseline for entity {} with {} events", entity.getId(), events.size());

        double avgAltitude = events.stream().mapToDouble(FlightEvent::getAltitude).average().orElse(0);
        double avgSpeed    = events.stream().mapToDouble(FlightEvent::getSpeed).average().orElse(0);
        double avgHeading  = events.stream().mapToDouble(FlightEvent::getHeading).average().orElse(0);
        double avgLat      = events.stream().mapToDouble(FlightEvent::getLat).average().orElse(0);
        double avgLon      = events.stream().mapToDouble(FlightEvent::getLon).average().orElse(0);

        Baseline baseline = baselineRepository.findByEntity(entity).orElse(new Baseline());
        baseline.setEntity(entity);
        baseline.setAvgAltitude(avgAltitude);
        baseline.setAvgSpeed(avgSpeed);
        baseline.setAvgHeading(avgHeading);
        baseline.setAvgLat(avgLat);
        baseline.setAvgLon(avgLon);
        baseline.setEventCount(events.size());
        baseline.setCalculatedAt(Instant.now());

        return Optional.of(baselineRepository.save(baseline));
    }

    public Optional<Baseline> getBaseline(AircraftEntity entity) {
        return baselineRepository.findByEntity(entity);
    }
}