package com.abel.sentinel.controller;

import com.abel.sentinel.dto.PositionDTO;
import com.abel.sentinel.model.AnomalyScore;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.model.PublicStatusDTO;
import com.abel.sentinel.repository.AircraftEntityRepository;
import com.abel.sentinel.repository.AnomalyScoreRepository;
import com.abel.sentinel.repository.FlightEventRepository;
import com.abel.sentinel.service.PositionService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/public")
public class PublicController {

    private final AircraftEntityRepository entityRepo;
    private final AnomalyScoreRepository anomalyRepo;
    private final FlightEventRepository eventRepo;
    private final PositionService positionService;

    public PublicController(AircraftEntityRepository entityRepo,
                            AnomalyScoreRepository anomalyRepo,
                            FlightEventRepository eventRepo,
                            PositionService positionService) {
        this.entityRepo = entityRepo;
        this.anomalyRepo = anomalyRepo;
        this.eventRepo = eventRepo;
        this.positionService = positionService;
    }

    @GetMapping("/status")
    public PublicStatusDTO getStatus() {
        Instant now = Instant.now();
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        long activeTracksNow = eventRepo.countDistinctEntityIdSince(fiveMinAgo);
        long anomaliesLastHour = anomalyRepo.countByFlaggedAtAfter(oneHourAgo);
        long totalEntities = entityRepo.count();

        List<AnomalyScore> recent = anomalyRepo.findTop10ByOrderByFlaggedAtDesc();
        List<PublicStatusDTO.RecentAnomaly> recentAnomalies = recent.stream().map(a ->
                new PublicStatusDTO.RecentAnomaly(
                        a.getId(),
                        a.getEntity().getCallsign(),
                        a.getEntity().getIcaoHex(),
                        a.getEntity().getClassification(),
                        a.getScore(),
                        a.getExplanation(),
                        a.getFlaggedAt().toString()
                )
        ).toList();

        List<PositionDTO> positions = positionService.getCurrentPositions();
        List<PublicStatusDTO.PublicPosition> publicPositions = positions.stream().map(p -> {
            double score = anomalyRepo
                    .findTopByEntityIdAndFlaggedAtAfterOrderByFlaggedAtDesc(p.entityId, fiveMinAgo)
                    .map(AnomalyScore::getScore)
                    .orElse(0.0);
            return new PublicStatusDTO.PublicPosition(
                    p.entityId, p.callsign, p.icaoHex,
                    p.lat, p.lon, p.altitude, p.speed, p.heading,
                    p.anomalous, score, null
            );
        }).toList();

        return new PublicStatusDTO(activeTracksNow, anomaliesLastHour, totalEntities,
                recentAnomalies, publicPositions);
    }

    @GetMapping("/track/{anomalyId}")
    public TrackDTO getTrack(@PathVariable Long anomalyId) {
        AnomalyScore anomaly = anomalyRepo.findById(anomalyId).orElseThrow();
        Instant flaggedAt = anomaly.getFlaggedAt();
        Instant start = flaggedAt.minus(30, ChronoUnit.MINUTES);
        Instant end = flaggedAt.plus(5, ChronoUnit.MINUTES);

        List<FlightEvent> events = eventRepo
                .findByEntityIdAndTimestampBetweenOrderByTimestampAsc(
                        anomaly.getEntity().getId(), start, end);

        List<TrackDTO.TrackPoint> points = events.stream().map(e ->
                new TrackDTO.TrackPoint(
                        e.getTimestamp().toString(),
                        e.getLat(), e.getLon(),
                        e.getAltitude(), e.getSpeed(), e.getHeading()
                )).toList();

        FlightEvent trigger = anomaly.getEvent();
        return new TrackDTO(
                anomaly.getId(),
                anomaly.getEntity().getCallsign(),
                anomaly.getEntity().getIcaoHex(),
                anomaly.getScore(),
                anomaly.getExplanation(),
                flaggedAt.toString(),
                trigger != null ? trigger.getLat() : null,
                trigger != null ? trigger.getLon() : null,
                points
        );
    }

    public record TrackDTO(
            Long anomalyId,
            String callsign,
            String icaoHex,
            double score,
            String explanation,
            String flaggedAt,
            Double triggerLat,
            Double triggerLon,
            List<TrackPoint> points
    ) {
        public record TrackPoint(
                String timestamp,
                Double lat,
                Double lon,
                Double altitude,
                Double speed,
                Double heading
        ) {}
    }
}