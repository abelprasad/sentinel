package com.abel.sentinel.repository;

import com.abel.sentinel.model.FlightEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface FlightEventRepository extends JpaRepository<FlightEvent, Long> {
    List<FlightEvent> findByEntityId(Long entityId);

    @Query("SELECT e FROM FlightEvent e WHERE e.id = (SELECT MAX(e2.id) FROM FlightEvent e2 WHERE e2.entityId = e.entityId)")
    List<FlightEvent> findLatestPerEntity();

    @Query("SELECT e FROM FlightEvent e WHERE e.id = (SELECT MAX(e2.id) FROM FlightEvent e2 WHERE e2.entityId = e.entityId) AND e.timestamp > :since")
    List<FlightEvent> findLatestPerEntitySince(@Param("since") Instant since);

    int deleteByTimestampBefore(Instant cutoff);
}