package com.abel.sentinel.service;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.FlightEvent;

public interface LlmService {
    String summarizeAnomaly(AircraftEntity entity, FlightEvent event, Baseline baseline, double score);
}