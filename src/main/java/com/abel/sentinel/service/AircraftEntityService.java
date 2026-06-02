package com.abel.sentinel.service;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.repository.AircraftEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AircraftEntityService {

    private final AircraftEntityRepository repository;

    public AircraftEntity create(AircraftEntity entity) {
        return repository.save(entity);
    }

    public List<AircraftEntity> getAll() {
        return repository.findAll();
    }

    public AircraftEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
    }


}
