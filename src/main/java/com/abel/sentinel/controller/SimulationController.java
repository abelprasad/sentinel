package com.abel.sentinel.controller;

import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/simulate")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * POST /simulate/quick
     * Picks a random entity with a baseline, injects a guaranteed score-1.0 event.
     */
    @PostMapping("/quick")
    public ResponseEntity<AnomalyScore> quickFire() {
        Optional<AnomalyScore> result = simulationService.quickFire();
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * POST /simulate/custom
     * Body: { entityId, altitude, speed, heading, lat (optional), lon (optional) }
     */
    @PostMapping("/custom")
    public ResponseEntity<AnomalyScore> custom(@RequestBody SimulateRequest request) {
        Optional<AnomalyScore> result = simulationService.custom(
                request.entityId(),
                request.altitude(),
                request.speed(),
                request.heading(),
                request.lat(),
                request.lon()
        );
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    public record SimulateRequest(
            Long entityId,
            Double altitude,
            Double speed,
            Double heading,
            Double lat,
            Double lon
    ) {}
}