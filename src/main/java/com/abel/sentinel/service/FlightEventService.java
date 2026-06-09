package com.abel.sentinel.service;

import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.FlightEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FlightEventService {

    private final FlightEventRepository repository;

    public FlightEventService(FlightEventRepository repository) {
        this.repository = repository;
    }

    public FlightEvent create(FlightEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        return repository.save(event);
    }

    public List<FlightEvent> getAll() {
        return repository.findAll();
    }

    public FlightEvent getById(Long id) {
        return repository.findById(id).orElse(null);
    }
}