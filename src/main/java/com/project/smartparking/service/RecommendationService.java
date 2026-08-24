package com.project.smartparking.service;

import com.project.smartparking.dto.RecommendationResponse;
import com.project.smartparking.model.SubscriptionPackage;
import com.project.smartparking.model.UserSubscription;
import com.project.smartparking.repository.SubscriptionPackageRepository;
import com.project.smartparking.repository.UserSubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private static final String GROQ_MODEL = "llama3-8b-8192";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPackageRepository packageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecommendationService(RestTemplate restTemplate,
                                 UserSubscriptionRepository userSubscriptionRepository,
                                 SubscriptionPackageRepository packageRepository) {
        this.restTemplate = restTemplate;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.packageRepository = packageRepository;
    }

    public RecommendationResponse getRecommendation(Long userId) {
        List<SubscriptionPackage> packages = packageRepository.findByActiveTrue();

        if (packages.isEmpty()) {
            log.warn("No active subscription packages found in DB for recommendation.");
            return new RecommendationResponse(null, "Standard Package", "No active packages currently available.");
        }

        if (groqApiKey == null || groqApiKey.isBlank() || groqApiKey.startsWith("${")) {
            log.warn("Groq API key is missing or unconfigured. Falling back to default package.");
            return fallback(packages);
        }

        List<UserSubscription> history = userSubscriptionRepository.findByUser_UserId(userId);

        String historySummary = history.isEmpty()
                ? "No previous subscriptions."
                : history.stream()
                    .map(s -> (s.getSubscriptionPackage() != null ? s.getSubscriptionPackage().getName() : "Unknown") 
                            + " (" + s.getStatus() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("No previous subscriptions.");

        String packageList = packages.stream()
                .map(p -> String.format("id=%d name=%s durationDays=%d price=%s",
                        p.getId(), p.getName(), p.getDurationDays(), p.getPrice()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String prompt = """
                A parking subscription user has this history: %s
                Available packages:
                %s
                Recommend exactly ONE package id that best fits this user.
                Respond with ONLY a JSON object, no markdown fences, no commentary:
                {"packageId": <id>, "reason": "<one short friendly sentence>"}
                """.formatted(historySummary, packageList);

        Map<String, Object> body = Map.of(
                "model", GROQ_MODEL,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(GROQ_URL, entity, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Groq API error — status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return fallback(packages);
        } catch (RestClientException e) {
            log.error("Groq API request failed", e);
            return fallback(packages);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return fallback(packages);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            String cleaned = content.replace("```json", "").replace("```", "").trim();

            JsonNode parsed = objectMapper.readTree(cleaned);
            Long pkgId = parsed.path("packageId").asLong();
            String reason = parsed.path("reason").asText();

            SubscriptionPackage pkg = packages.stream()
                    .filter(p -> p.getId().equals(pkgId))
                    .findFirst()
                    .orElse(packages.get(0));

            return new RecommendationResponse(pkg.getId(), pkg.getName(), reason);
        } catch (Exception e) {
            log.error("Failed to parse Groq recommendation response", e);
            return fallback(packages);
        }
    }

    private RecommendationResponse fallback(List<SubscriptionPackage> packages) {
        SubscriptionPackage pkg = packages.get(0);
        return new RecommendationResponse(pkg.getId(), pkg.getName(), "A solid choice to get started.");
    }
}
