package com.project.smartparking.controller;

import com.project.smartparking.dto.NotificationSendRequest;
import com.project.smartparking.model.NotificationBroadcast;
import com.project.smartparking.model.User;
import com.project.smartparking.service.NotificationBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/broadcasts")
@PreAuthorize("hasRole('ADMIN') or hasRole('HUB_ADMIN')")
public class NotificationBroadcastController {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastController.class);

    @Autowired
    private NotificationBroadcastService broadcastService;

    @GetMapping
    public ResponseEntity<?> getAll(Authentication authentication) {
        try {
            User principal = (User) authentication.getPrincipal();
            if (principal.getRole() == User.Role.HUB_ADMIN) {
                if (principal.getHub() == null) {
                    return ResponseEntity.status(403).body("Hub admin has no assigned hub");
                }
                return ResponseEntity.ok(broadcastService.getAllByHub(principal.getHub().getHubId()));
            }
            return ResponseEntity.ok(broadcastService.getAll());
        } catch (Exception e) {
            log.error("GET /api/notifications/broadcasts failed", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to load broadcasts: " + e.getMessage());
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody NotificationSendRequest req, Authentication authentication) {
        try {
            User caller = (User) authentication.getPrincipal();
            NotificationBroadcast result = broadcastService.send(req, caller);
            return ResponseEntity.ok(result);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("POST /api/notifications/broadcasts/send failed", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send notification: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<?> resend(@PathVariable Long id, Authentication authentication) {
        try {
            User caller = (User) authentication.getPrincipal();
            if (!canAccessBroadcast(id, caller)) {
                return ResponseEntity.status(403).body("Not your hub's broadcast");
            }
            return ResponseEntity.ok(broadcastService.resend(id, caller));
        } catch (Exception e) {
            log.error("POST /api/notifications/broadcasts/{}/resend failed", id, e);
            return ResponseEntity.internalServerError()
                    .body("Resend failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        try {
            User caller = (User) authentication.getPrincipal();
            if (!canAccessBroadcast(id, caller)) {
                return ResponseEntity.status(403).body("Not your hub's broadcast");
            }
            broadcastService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("DELETE /api/notifications/broadcasts/{} failed", id, e);
            return ResponseEntity.internalServerError()
                    .body("Delete failed: " + e.getMessage());
        }
    }

    private boolean canAccessBroadcast(Long id, User caller) {
        if (caller.getRole() != User.Role.HUB_ADMIN)
            return true; // ADMIN unrestricted
        NotificationBroadcast b = broadcastService.getById(id);
        return b != null && caller.getHub() != null
                && caller.getHub().getHubId().equals(b.getHubId());
    }
}