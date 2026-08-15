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
import com.project.smartparking.dto.SubscriptionSummaryResponse;

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

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<SubscriptionSummaryResponse> getSubscriptionSummary() {
        return ResponseEntity.ok(subscriptionService.getSubscriptionSummary());
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

    @GetMapping("/admin/packages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionPackage>> getAllPackagesAdmin() {
        return ResponseEntity.ok(subscriptionService.getAllPackages());
    }

    @PostMapping("/admin/packages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPackage> createPackage(@RequestBody SubscriptionPackage pkg) {
        return ResponseEntity.ok(subscriptionService.createPackage(pkg));
    }

    @PutMapping("/admin/packages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPackage> updatePackage(
            @PathVariable Long id,
            @RequestBody SubscriptionPackage pkg) {
        try {
            return ResponseEntity.ok(subscriptionService.updatePackage(id, pkg));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/admin/packages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        try {
            subscriptionService.deletePackage(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/user/{userId}/location")
    @PreAuthorize("@securityAccess.isOwner(authentication, #userId)")
    public ResponseEntity<UserSubscription> updateLocation(
            @PathVariable Long userId,
            @RequestBody com.project.smartparking.dto.LocationUpdateRequest request) {
        try {
            UserSubscription updated = subscriptionService.updateLocation(userId, request);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSubscription>> getAllSubscriptionsAdmin() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }
}