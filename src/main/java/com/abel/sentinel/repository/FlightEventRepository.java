package com.abel.sentinel.repository;

import com.abel.sentinel.model.FlightEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlightEventRepository extends JpaRepository<FlightEvent, Long> {
    List<FlightEvent> findByEntityId(Long entityId);
}