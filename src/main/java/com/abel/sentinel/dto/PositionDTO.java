package com.abel.sentinel.dto;

public class PositionDTO {
    public Long entityId;
    public String callsign;
    public String icaoHex;
    public Double lat;
    public Double lon;
    public Double altitude;
    public Double speed;
    public Double heading;
    public boolean anomalous;

    public PositionDTO(Long entityId, String callsign, String icaoHex,
                       Double lat, Double lon, Double altitude,
                       Double speed, Double heading, boolean anomalous) {
        this.entityId = entityId;
        this.callsign = callsign;
        this.icaoHex = icaoHex;
        this.lat = lat;
        this.lon = lon;
        this.altitude = altitude;
        this.speed = speed;
        this.heading = heading;
        this.anomalous = anomalous;
    }
}