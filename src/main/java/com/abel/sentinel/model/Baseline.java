package com.abel.sentinel.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "baselines")
@Data
public class Baseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private AircraftEntity entity;

    private Double avgAltitude;
    private Double avgSpeed;
    private Double avgHeading;
    private Double avgLat;
    private Double avgLon;

    private Integer eventCount;
    private Instant calculatedAt;
}