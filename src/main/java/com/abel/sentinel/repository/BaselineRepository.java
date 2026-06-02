package com.abel.sentinel.repository;

import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.AircraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BaselineRepository extends JpaRepository<Baseline, Long> {
    Optional<Baseline> findByEntity(AircraftEntity entity);
}