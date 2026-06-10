package com.abel.sentinel.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "anomalies")
@Data
public class AnomalyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "entity_id", nullable = false)
    private AircraftEntity entity;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private FlightEvent event;

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private Instant flaggedAt;
}