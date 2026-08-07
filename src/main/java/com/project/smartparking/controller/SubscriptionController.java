package com.project.smartparking.controller;

import com.project.smartparking.dto.RecommendationResponse;
import com.project.smartparking.dto.SubscriptionPurchaseRequest;
import com.project.smartparking.model.SubscriptionPackage;
import com.project.smartparking.model.UserSubscription;
import com.project.smartparking.service.RecommendationService;
import com.project.smartparking.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/packages")
    public ResponseEntity<List<SubscriptionPackage>> getActivePackages() {
        return ResponseEntity.ok(subscriptionService.getActivePackages());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@securityAccess.isOwner(authentication, #userId)")
    public ResponseEntity<List<UserSubscription>> getUserSubscriptions(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(userId));
    }

    @GetMapping("/user/{userId}/active")
    @PreAuthorize("@securityAccess.isOwner(authentication, #userId)")
    public ResponseEntity<UserSubscription> getActiveSubscription(@PathVariable Long userId) {
        UserSubscription active = subscriptionService.getActiveSubscription(userId);
        if (active != null) {
            return ResponseEntity.ok(active);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/purchase")
    @PreAuthorize("@securityAccess.isOwner(authentication, #request.userId)")
    public ResponseEntity<UserSubscription> purchasePackage(@RequestBody SubscriptionPurchaseRequest request) {
        try {
            UserSubscription sub = subscriptionService.purchasePackage(request);
            return ResponseEntity.ok(sub);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}/recommendation")
    @PreAuthorize("@securityAccess.isOwner(authentication, #userId)")
    public ResponseEntity<RecommendationResponse> getRecommendation(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(recommendationService.getRecommendation(userId));
        } catch (Exception e) {
            return ResponseEntity.status(503).build();
        }
    }
}