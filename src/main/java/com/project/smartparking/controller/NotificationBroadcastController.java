package com.project.smartparking.controller;

import com.project.smartparking.dto.NotificationSendRequest;
import com.project.smartparking.model.NotificationBroadcast;
import com.project.smartparking.service.NotificationBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications/broadcasts")
@PreAuthorize("hasRole('ADMIN') or hasRole('HUB_ADMIN')")
public class NotificationBroadcastController {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastController.class);

    @Autowired
    private NotificationBroadcastService broadcastService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(broadcastService.getAll());
        } catch (Exception e) {
            log.error("GET /api/notifications/broadcasts failed", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to load broadcasts: " + e.getMessage());
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody NotificationSendRequest req) {
        try {
            NotificationBroadcast result = broadcastService.send(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("POST /api/notifications/broadcasts/send failed", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send notification: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<?> resend(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(broadcastService.resend(id));
        } catch (Exception e) {
            log.error("POST /api/notifications/broadcasts/{}/resend failed", id, e);
            return ResponseEntity.internalServerError()
                    .body("Resend failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            broadcastService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("DELETE /api/notifications/broadcasts/{} failed", id, e);
            return ResponseEntity.internalServerError()
                    .body("Delete failed: " + e.getMessage());
        }
    }
}