package com.abel.sentinel.service;

import com.abel.sentinel.model.AircraftEntity;
import com.abel.sentinel.model.Baseline;
import com.abel.sentinel.model.FlightEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "llm.provider", havingValue = "groq")
public class GroqLlmService implements LlmService {

    private final ObjectMapper objectMapper;

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Override
    public String summarizeAnomaly(AircraftEntity entity, FlightEvent event, Baseline baseline, double score) {
        try {
            String prompt = buildPrompt(entity, event, baseline, score);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 100);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.set("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = new RestTemplate().postForEntity(GROQ_URL, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.warn("Groq summarization failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(AircraftEntity entity, FlightEvent event, Baseline baseline, double score) {
        return String.format(
                "You are an air traffic anomaly analyst. In one sentence, explain this anomaly concisely.\n\n" +
                        "Aircraft: %s (ICAO: %s)\n" +
                        "Anomaly score: %.2f / 1.0\n" +
                        "Current altitude: %.0f ft, baseline: %.0f ft\n" +
                        "Current speed: %.0f kts, baseline: %.0f kts\n" +
                        "Current heading: %.0f°, baseline: %.0f°\n\n" +
                        "One sentence only. No preamble.",
                entity.getCallsign(), entity.getIcaoHex(),
                score,
                event.getAltitude(), baseline.getAvgAltitude(),
                event.getSpeed(), baseline.getAvgSpeed(),
                event.getHeading(), baseline.getAvgHeading()
        );
    }
}