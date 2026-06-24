package com.abel.sentinel.repository;

import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.model.AircraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnomalyScoreRepository extends JpaRepository<AnomalyScore, Long> {
    List<AnomalyScore> findByEntityOrderByFlaggedAtDesc(AircraftEntity entity);
    List<AnomalyScore> findByEntityAndScoreGreaterThanOrderByFlaggedAtDesc(AircraftEntity entity, Double threshold);
    List<AnomalyScore> findByScoreGreaterThanOrderByFlaggedAtDesc(Double threshold);
    boolean existsByEntityAndFlaggedAtAfter(AircraftEntity entity, Instant since);
    int deleteByFlaggedAtBefore(Instant cutoff);
    long countByFlaggedAtAfter(Instant instant);
    List<AnomalyScore> findTop10ByOrderByFlaggedAtDesc();
    Optional<AnomalyScore> findTopByEntityIdAndFlaggedAtAfterOrderByFlaggedAtDesc(Long entityId, Instant after);
}