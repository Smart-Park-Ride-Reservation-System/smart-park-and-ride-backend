package com.project.smartparking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.project.smartparking.model.Notification;
import com.project.smartparking.repository.NotificationRepository;
import com.project.smartparking.service.NotificationService;
import java.util.List;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:3000") // adjust for your Next.js dev URL in production
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Get all notifications for a user (e.g., for a notification bell/inbox)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    // Mark a notification as read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
