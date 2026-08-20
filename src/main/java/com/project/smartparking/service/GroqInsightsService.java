package com.project.smartparking.service;

import com.project.smartparking.dto.AiInsightsDtos.AiInsightsRequest;
import com.project.smartparking.dto.AiInsightsDtos.AiInsightsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GroqInsightsService {

    private static final Logger log = LoggerFactory.getLogger(GroqInsightsService.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    // Updated model identifier to active Groq model
    private static final String GROQ_MODEL = "openai/gpt-oss-120b";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String RESPONSE_SCHEMA = """
            {
              "summary": string,
              "insights": string[],
              "anomalies": [
                { "severity": "low" | "medium" | "high", "message": string }
              ],
              "forecast": {
                "predictedBookings": number,
                "predictedRevenue": number,
                "trend": "up" | "down" | "flat",
                "confidence": "low" | "medium" | "high",
                "note": string
              }
            }
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Simple in-memory cache
    private static final long CACHE_TTL_MILLIS = 2 * 60 * 1000;
    private final Map<String, CacheEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();

    private record CacheEntry(AiInsightsResponse response, long timestamp) {
    }

    public GroqInsightsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AiInsightsResponse generateInsights(AiInsightsRequest request) throws Exception {
        String cacheKey = objectMapper.writeValueAsString(request);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < CACHE_TTL_MILLIS) {
            return cached.response();
        }

        String prompt = buildPrompt(request);

        Map<String, Object> body = Map.of(
                "model", GROQ_MODEL,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt)),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = GROQ_URL;

        ResponseEntity<String> response = null;
        int maxAttempts = 3;
        long backoffMillis = 2000L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                response = restTemplate.postForEntity(url, entity, String.class);
                break; // success
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == maxAttempts) {
                    throw new AiInsightsException(
                            "AI provider rate limit hit — try again in a minute", e);
                }
                try {
                    Thread.sleep(backoffMillis * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (HttpClientErrorException e) {
                log.error("Groq API error — status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new AiInsightsException(
                        "Groq error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
            } catch (RestClientException e) {
                log.error("Groq API request failed", e);
                throw new AiInsightsException("AI provider request failed: " + e.getMessage(), e);
            }
        }

        if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AiInsightsException("AI provider returned status "
                    + (response != null ? response.getStatusCode() : "unknown"));
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode messageNode = root.path("choices").path(0).path("message").path("content");

        if (messageNode.isMissingNode() || messageNode.asText().isBlank()) {
            throw new AiInsightsException("AI provider returned an empty response");
        }

        String cleaned = messageNode.asText()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        AiInsightsResponse parsed;
        try {
            parsed = objectMapper.readValue(cleaned, AiInsightsResponse.class);
        } catch (Exception e) {
            throw new AiInsightsException("AI response was not valid JSON: " + cleaned, e);
        }

        cache.put(cacheKey, new CacheEntry(parsed, System.currentTimeMillis()));
        return parsed;
    }

    private String buildPrompt(AiInsightsRequest request) throws Exception {
        String dataJson = objectMapper.writeValueAsString(request);

        return """
                You are a data analyst for a smart parking system. Analyze the aggregated
                stats below for the "%s" period and respond with ONLY a single JSON object —
                no markdown fences, no commentary, matching exactly this shape:

                %s

                Rules:
                - Base every claim strictly on the numbers provided. Never invent figures.
                - Insights should call out things a busy admin would want to know (growth/decline,
                  concentration in one status/vehicle type/slot, payment friction, sensor issues).
                - Only include "anomalies" for genuinely unusual patterns (e.g. a spike/drop, a
                  slot or vehicle type dominating usage, an elevated failed/pending payment rate).
                - The forecast should extrapolate from the current vs. previous period trend
                  in "growth". If previous-period data is missing or zero, say confidence is "low".
                - Important: Please format your response as a valid JSON object matching the schema.

                DATA:
                %s
                """.formatted(request.getDateRange(), RESPONSE_SCHEMA, dataJson);
    }

    public static class AiInsightsException extends Exception {
        public AiInsightsException(String message) {
            super(message);
        }

        public AiInsightsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}