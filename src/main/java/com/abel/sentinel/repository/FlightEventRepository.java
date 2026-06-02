package com.abel.sentinel.repository;

import com.abel.sentinel.model.FlightEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightEventRepository extends JpaRepository<FlightEvent, Long> {
}