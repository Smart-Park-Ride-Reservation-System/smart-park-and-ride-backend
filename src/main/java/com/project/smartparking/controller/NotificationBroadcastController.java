package com.project.smartparking.controller;

import com.project.smartparking.dto.NotificationSendRequest;
import com.project.smartparking.model.NotificationBroadcast;
import com.project.smartparking.service.NotificationBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationBroadcastController {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastController.class);

    @Autowired
    private NotificationBroadcastService broadcastService;

    @GetMapping
    public ResponseEntity<List<NotificationBroadcast>> getAll() {
        return ResponseEntity.ok(broadcastService.getAll());
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody NotificationSendRequest req) {
        try {
            NotificationBroadcast result = broadcastService.send(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("POST /api/notifications/send failed", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send notification: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<?> resend(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(broadcastService.resend(id));
        } catch (Exception e) {
            log.error("POST /api/notifications/{}/resend failed", id, e);
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
            log.error("DELETE /api/notifications/{} failed", id, e);
            return ResponseEntity.internalServerError()
                    .body("Delete failed: " + e.getMessage());
        }
    }
}