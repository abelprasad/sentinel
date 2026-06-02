package com.abel.sentinel.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "events")
@Data
public class FlightEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private Instant timestamp;

    private Double lat;
    private Double lon;
    private Double altitude;
    private Double speed;
    private Double heading;

}