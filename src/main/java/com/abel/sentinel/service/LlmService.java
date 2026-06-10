package com.abel.sentinel.service;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.FlightEvent;

import java.util.concurrent.CompletableFuture;

public interface LlmService {
    CompletableFuture<String> summarizeAnomaly(AircraftEntity entity, FlightEvent event, Baseline baseline, double score);
}