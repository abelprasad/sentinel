package com.abel.sentinel.controller;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.service.AircraftEntityService;
import com.abel.sentinel.service.AnomalyScoreService;
import com.abel.sentinel.service.BaselineService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/entities")
public class AircraftEntityController {

    private final AircraftEntityService service;
    private final BaselineService baselineService;
    private final AnomalyScoreService anomalyScoreService;

    public AircraftEntityController(AircraftEntityService service,
                                    BaselineService baselineService,
                                    AnomalyScoreService anomalyScoreService) {
        this.service = service;
        this.baselineService = baselineService;
        this.anomalyScoreService = anomalyScoreService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AircraftEntity create(@RequestBody AircraftEntity entity) {
        return service.create(entity);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'OPERATOR', 'ADMIN')")
    public List<AircraftEntity> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'OPERATOR', 'ADMIN')")
    public AircraftEntity getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/baseline")
    @PreAuthorize("hasAnyRole('ANALYST', 'OPERATOR', 'ADMIN')")
    public Baseline getBaseline(@PathVariable Long id) {
        AircraftEntity entity = service.getById(id);
        return baselineService.getBaseline(entity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No baseline found for entity " + id));
    }

    @GetMapping("/{id}/anomalies")
    @PreAuthorize("hasAnyRole('ANALYST', 'OPERATOR', 'ADMIN')")
    public List<AnomalyScore> getAnomalies(@PathVariable Long id) {
        AircraftEntity entity = service.getById(id);
        return anomalyScoreService.getAnomaliesForEntity(entity);
    }
}