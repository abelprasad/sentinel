package com.abel.sentinel.service;

import com.abel.sentinel.model.AdsbAircraft;
import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.FlightEvent;
import com.abel.sentinel.repository.AircraftEntityRepository;
import com.abel.sentinel.repository.FlightEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdsbIngestionService {

    private final AircraftEntityRepository entityRepository;
    private final FlightEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${adsb.url}")
    private String adsbUrl;

    @Scheduled(fixedDelayString = "${adsb.poll-interval-ms}")
    public void ingest() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(adsbUrl, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode acArray = root.path("aircraft");

            if (!acArray.isArray()) {
                log.warn("adsb.fi response missing 'aircraft' array");
                return;
            }

            List<AdsbAircraft> aircraft = objectMapper.convertValue(
                    acArray,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AdsbAircraft.class)
            );

            int ingested = 0;
            for (AdsbAircraft ac : aircraft) {
                if (ac.getHex() == null || ac.getLat() == null || ac.getLon() == null) {
                    continue; // skip incomplete records
                }

                // find or create entity
                AircraftEntity entity = entityRepository
                        .findByIcaoHex(ac.getHex())
                        .orElseGet(() -> {
                            AircraftEntity newEntity = new AircraftEntity();
                            newEntity.setIcaoHex(ac.getHex());
                            newEntity.setCallsign(ac.getFlight() != null ? ac.getFlight().trim() : ac.getHex());
                            newEntity.setType("AIRCRAFT");
                            newEntity.setMetadata("auto-registered via ADS-B ingestion");
                            return entityRepository.save(newEntity);
                        });

                // parse altitude -- "ground" becomes 0.0
                Double altitude = 0.0;
                if (ac.getAltBaro() instanceof Number) {
                    altitude = ((Number) ac.getAltBaro()).doubleValue();
                }

                // build and save event
                FlightEvent event = new FlightEvent();
                event.setEntityId(entity.getId());
                event.setTimestamp(Instant.now());
                event.setLat(ac.getLat());
                event.setLon(ac.getLon());
                event.setAltitude(altitude);
                event.setSpeed(ac.getGs());
                event.setHeading(ac.getTrack());
                eventRepository.save(event);
                ingested++;
            }

            log.info("ADS-B ingestion complete -- {} events saved", ingested);

        } catch (Exception e) {
            log.error("ADS-B ingestion failed: {}", e.getMessage());
        }
    }
}