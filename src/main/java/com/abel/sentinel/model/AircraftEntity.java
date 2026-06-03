package com.abel.sentinel.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "entities")
public class AircraftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String callsign;
    private String metadata;

    @Column(unique = true)
    private String icaoHex;
}