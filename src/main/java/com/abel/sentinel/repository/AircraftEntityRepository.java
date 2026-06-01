package com.abel.sentinel.repository;

import com.abel.sentinel.model.AircraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AircraftEntityRepository extends JpaRepository<AircraftEntity, Long> {
}