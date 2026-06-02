package com.abel.sentinel.controller;

import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.service.FlightEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class FlightEventController {

    private final FlightEventService service;

    public FlightEventController(FlightEventService service) {
        this.service = service;
    }

    @PostMapping
    public FlightEvent create(@RequestBody FlightEvent event) {
        return service.create(event);
    }

    @GetMapping
    public List<FlightEvent> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FlightEvent getById(@PathVariable Long id) {
        return service.getById(id);
    }
}