// Place in your service package, e.g. com.project.smartparking.service

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
public class GeminiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(GeminiInsightsService.class);

    // Free tier — get a key at https://aistudio.google.com/apikey (no card required)
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_MODEL = "gemini-2.0-flash";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent";

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

    // Simple in-memory cache: same aggregated payload within this window
    // reuses the last AI response instead of calling Gemini again.
    private static final long CACHE_TTL_MILLIS = 2 * 60 * 1000; // 2 minutes
    private final java.util.Map<String, CacheEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();

    private record CacheEntry(AiInsightsResponse response, long timestamp) {}

    public GeminiInsightsService(RestTemplate restTemplate) {
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
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = GEMINI_URL + "?key=" + geminiApiKey;

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
                            "AI provider rate limit hit — try again in a minute (free tier quota)", e);
                }
                try {
                    Thread.sleep(backoffMillis * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (HttpClientErrorException e) {
                log.error("Gemini API error — status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new AiInsightsException(
                    "Gemini error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
            } catch (RestClientException e) {
                log.error("Gemini API request failed", e);
                throw new AiInsightsException("AI provider request failed: " + e.getMessage(), e);
            }
        }

        if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AiInsightsException("AI provider returned status "
                    + (response != null ? response.getStatusCode() : "unknown"));
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new AiInsightsException("AI provider returned an empty response");
        }

        String cleaned = textNode.asText()
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

            DATA:
            %s
            """.formatted(request.getDateRange(), RESPONSE_SCHEMA, dataJson);
    }

    public static class AiInsightsException extends Exception {
        public AiInsightsException(String message) { super(message); }
        public AiInsightsException(String message, Throwable cause) { super(message, cause); }
    }
}