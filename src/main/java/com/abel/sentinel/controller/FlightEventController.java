package com.abel.sentinel.controller;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.service.AnomalyScoreService;
import com.abel.sentinel.service.BaselineService;
import com.abel.sentinel.service.FlightEventService;
import com.abel.sentinel.repository.AircraftEntityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/events")
public class FlightEventController {

    private final FlightEventService flightEventService;
    private final BaselineService baselineService;
    private final AnomalyScoreService anomalyScoreService;
    private final AircraftEntityRepository entityRepository;

    public FlightEventController(FlightEventService flightEventService,
                                 BaselineService baselineService,
                                 AnomalyScoreService anomalyScoreService,
                                 AircraftEntityRepository entityRepository) {
        this.flightEventService = flightEventService;
        this.baselineService = baselineService;
        this.anomalyScoreService = anomalyScoreService;
        this.entityRepository = entityRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public FlightEvent create(@RequestBody FlightEvent event) {
        FlightEvent saved = flightEventService.create(event);

        AircraftEntity entity = entityRepository.findById(saved.getEntityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));

        anomalyScoreService.score(entity, saved);
        baselineService.calculate(entity);

        return saved;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'OPERATOR', 'ADMIN')")
    public List<FlightEvent> getAll() {
        return flightEventService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'OPERATOR', 'ADMIN')")
    public FlightEvent getById(@PathVariable Long id) {
        return flightEventService.getById(id);
    }
}