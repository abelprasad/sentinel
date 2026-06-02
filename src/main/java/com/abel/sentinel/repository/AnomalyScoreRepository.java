package com.abel.sentinel.repository;

import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.model.AircraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnomalyScoreRepository extends JpaRepository<AnomalyScore, Long> {
    List<AnomalyScore> findByEntityOrderByFlaggedAtDesc(AircraftEntity entity);
    List<AnomalyScore> findByEntityAndScoreGreaterThanOrderByFlaggedAtDesc(AircraftEntity entity, Double threshold);
}