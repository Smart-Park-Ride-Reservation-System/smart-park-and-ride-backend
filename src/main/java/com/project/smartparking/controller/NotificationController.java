package com.project.smartparking.controller;

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

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('COMMUTER') and #userId == authentication.principal.id)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('COMMUTER') and #userId == authentication.principal.id)")
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnread(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('HUB_ADMIN') or hasRole('COMMUTER')")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    private Long getUserIdFromAuth(Authentication auth) {
        com.project.smartparking.model.User user = (com.project.smartparking.model.User) auth.getPrincipal();
        return user.getUserId();
    }
}