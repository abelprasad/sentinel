package com.abel.sentinel.controller;

import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.service.AnomalyScoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/anomalies")
public class AnomalyController {

    private final AnomalyScoreService anomalyScoreService;

    public AnomalyController(AnomalyScoreService anomalyScoreService) {
        this.anomalyScoreService = anomalyScoreService;
    }

    @GetMapping
    public List<AnomalyScore> getAll(@RequestParam(required = false) String severity) {
        if ("HIGH".equalsIgnoreCase(severity)) {
            return anomalyScoreService.getHighSeverityAnomalies();
        }
        return anomalyScoreService.getAllAnomalies();
    }
}