package com.project.smartparking.controller;

import com.project.smartparking.Config.AuthenticatedUser;
import com.project.smartparking.model.Notification;
import com.project.smartparking.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/user-notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * GET /api/user-notifications
     * Returns notifications for the currently authenticated user.
     * Works for COMMUTER, ATTENDANT, HUB_ADMIN, ADMIN.
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        Long userId = getPrincipalUserId(authentication);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    /**
     * GET /api/user-notifications/unread
     * Unread count for the top-nav badge — any authenticated user.
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getMyUnread(Authentication authentication) {
        Long userId = getPrincipalUserId(authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    /**
     * GET /api/user-notifications/user/{userId}
     * Admin can query any user; commuters can only query themselves.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(
            @PathVariable Long userId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        // Commuters can only access their own notifications
        if ("COMMUTER".equals(principal.role()) && !principal.userId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    /**
     * GET /api/user-notifications/user/{userId}/unread
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnread(
            @PathVariable Long userId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        if ("COMMUTER".equals(principal.role()) && !principal.userId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    /**
     * PATCH /api/user-notifications/{id}/read
     * Mark a notification as read.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Long getPrincipalUserId(Authentication auth) {
        // Principal is always AuthenticatedUser (our JWT record type)
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        return principal.userId();
    }
}