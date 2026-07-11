package com.project.smartparking.controller;

import com.project.smartparking.dto.AiInsightsDtos.AiInsightsRequest;
import com.project.smartparking.dto.AiInsightsDtos.AiInsightsResponse;
import com.project.smartparking.service.GroqInsightsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-insights")
public class AiInsightsController {

    private final GroqInsightsService groqInsightsService;

    public AiInsightsController(GroqInsightsService groqInsightsService) {
        this.groqInsightsService = groqInsightsService;
    }

    // Secure this the same way your other admin-only endpoints are secured —
    // e.g. add @PreAuthorize("hasRole('ADMIN')") if you're using Spring Security
    // method security, or rely on your existing JWT filter chain for this path.
    @PostMapping
    public ResponseEntity<?> getInsights(@RequestBody AiInsightsRequest request) {
        try {
            AiInsightsResponse insights = groqInsightsService.generateInsights(request);
            return ResponseEntity.ok(insights);
        } catch (GroqInsightsService.AiInsightsException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected server error"));
        }
    }
}
