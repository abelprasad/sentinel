package com.abel.sentinel.controller;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.service.AircraftEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/entities")
@RequiredArgsConstructor
public class AircraftEntityController {

    private final AircraftEntityService service;

    @PostMapping
    public AircraftEntity create(@RequestBody AircraftEntity entity) {
        return service.create(entity);
    }

    @GetMapping
    public List<AircraftEntity> getAll(){
        return service.getAll();
    }

    @GetMapping("/{id}")
    public AircraftEntity getById(@PathVariable Long id) {
        return service.getById(id);
    }



}
