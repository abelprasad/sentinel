package com.abel.sentinel.repository;

import com.abel.sentinel.model.AircraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AircraftEntityRepository extends JpaRepository<AircraftEntity, Long> {
    Optional<AircraftEntity> findByIcaoHex(String icaoHex);
}