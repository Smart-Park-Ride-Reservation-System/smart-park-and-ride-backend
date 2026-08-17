package com.project.smartparking.service;

import com.project.smartparking.dto.NotificationSendRequest;
import com.project.smartparking.model.NotificationBroadcast;
import com.project.smartparking.model.NotificationType;
import com.project.smartparking.model.User;
import com.project.smartparking.model.Hub;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.repository.NotificationBroadcastRepository;
import com.project.smartparking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class NotificationBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastService.class);

    @Autowired
    private NotificationBroadcastRepository broadcastRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepo;

    public List<NotificationBroadcast> getAll() {
        return broadcastRepo.findAllByOrderByCreatedAtDesc();
    }

    public NotificationBroadcast send(NotificationSendRequest req) {
        log.info("Broadcast requested: title='{}' audience={} hubId={} scheduledFor={}",
                req.getTitle(), req.getAudience(), req.getHubId(), req.getScheduledFor());

        NotificationBroadcast broadcast = new NotificationBroadcast();
        broadcast.setTitle(req.getTitle());
        broadcast.setMessage(req.getMessage());
        broadcast.setDisplayType(req.getType());
        broadcast.setAudience(req.getAudience());
        broadcast.setHubId(req.getHubId());
        broadcast.setSingleUserId(req.getUserId());

        if (req.getScheduledFor() != null && !req.getScheduledFor().isBlank()) {
            try {
                Timestamp scheduled = Timestamp.valueOf(
                        LocalDateTime.parse(req.getScheduledFor()));
                broadcast.setScheduledFor(scheduled);
                broadcast.setStatus("SCHEDULED");
                broadcast.setRecipientCount(0);
                broadcast.setSendEmail(req.isSendEmail()); 
                broadcast.setAudienceLabel(buildAudienceLabel(req));
                return broadcastRepo.save(broadcast);
            } catch (DateTimeParseException e) {
                log.error("Bad scheduledFor value '{}': {}", req.getScheduledFor(), e.getMessage());
                broadcast.setStatus("FAILED");
                broadcast.setFailureReason("Invalid scheduledFor format: " + req.getScheduledFor());
                return broadcastRepo.save(broadcast);
            }
        }

        // Send now
        try {
            List<User> recipients = resolveRecipients(req);
            log.info("Resolved {} recipients for audience={}", recipients.size(), req.getAudience());

            if (recipients.isEmpty()) {
                broadcast.setStatus("FAILED");
                broadcast.setRecipientCount(0);
                broadcast.setFailureReason("No recipients found for audience=" + req.getAudience()
                        + (req.getHubId() != null ? " hubId=" + req.getHubId() : ""));
                broadcast.setAudienceLabel(buildAudienceLabel(req));
                return broadcastRepo.save(broadcast);
            }

            int count = notificationService.notifyMany(
                    recipients, req.getTitle(), req.getMessage(), NotificationType.SYSTEM, req.isSendEmail());

            broadcast.setStatus("SENT");
            broadcast.setRecipientCount(count);
            broadcast.setSentAt(Timestamp.valueOf(LocalDateTime.now()));
            broadcast.setAudienceLabel(buildAudienceLabel(req));
            return broadcastRepo.save(broadcast);

        } catch (Exception e) {
            // IMPORTANT: log the real stack trace instead of swallowing it.
            log.error("Broadcast send failed", e);
            broadcast.setStatus("FAILED");
            broadcast.setRecipientCount(0);
            broadcast.setFailureReason(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            broadcast.setAudienceLabel(buildAudienceLabel(req));
            return broadcastRepo.save(broadcast);
        }
    }

    public NotificationBroadcast resend(Long broadcastId) {
        NotificationBroadcast old = broadcastRepo.findById(broadcastId)
                .orElseThrow(() -> new RuntimeException("Broadcast not found: " + broadcastId));

        NotificationSendRequest req = new NotificationSendRequest();
        req.setTitle(old.getTitle());
        req.setMessage(old.getMessage());
        req.setType(old.getDisplayType());
        req.setAudience(old.getAudience());
        req.setHubId(old.getHubId());
        req.setUserId(old.getSingleUserId());
        req.setScheduledFor(null);
        req.setSendEmail(old.isSendEmail());

        return send(req);
    }

    public void delete(Long broadcastId) {
        broadcastRepo.deleteById(broadcastId);
    }

    public void dispatchScheduled(NotificationBroadcast broadcast) {
        log.info("Dispatching scheduled broadcast ID={}: title='{}'", broadcast.getId(), broadcast.getTitle());
        try {
            NotificationSendRequest req = new NotificationSendRequest();
            req.setTitle(broadcast.getTitle());
            req.setMessage(broadcast.getMessage());
            req.setType(broadcast.getDisplayType());
            req.setAudience(broadcast.getAudience());
            req.setHubId(broadcast.getHubId());
            req.setUserId(broadcast.getSingleUserId());
            req.setScheduledFor(null);

            List<User> recipients = resolveRecipients(req);
            log.info("Resolved {} recipients for scheduled broadcast", recipients.size());

            if (recipients.isEmpty()) {
                broadcast.setStatus("FAILED");
                broadcast.setRecipientCount(0);
                broadcast.setFailureReason("No recipients found for audience=" + broadcast.getAudience()
                        + (broadcast.getHubId() != null ? " hubId=" + broadcast.getHubId() : ""));
                broadcastRepo.save(broadcast);
                return;
            }

            int count = notificationService.notifyMany(
                    recipients, broadcast.getTitle(), broadcast.getMessage(), NotificationType.SYSTEM,
                    broadcast.isSendEmail());
            broadcast.setStatus("SENT");
            broadcast.setRecipientCount(count);
            broadcast.setSentAt(Timestamp.valueOf(LocalDateTime.now()));
            broadcastRepo.save(broadcast);

        } catch (Exception e) {
            log.error("Failed to dispatch scheduled broadcast ID=" + broadcast.getId(), e);
            broadcast.setStatus("FAILED");
            broadcast.setRecipientCount(0);
            broadcast.setFailureReason(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            broadcastRepo.save(broadcast);
        }
    }

    private List<User> resolveRecipients(NotificationSendRequest req) {
        switch (req.getAudience()) {
            case "ALL_USERS":
                return userRepo.findAll();
            case "HUB":
                if (req.getHubId() == null) {
                    throw new IllegalArgumentException("hubId is required when audience=HUB");
                }
                return userRepo.findByHubId(req.getHubId());
            case "SINGLE_USER":
                if (req.getUserId() == null) {
                    throw new IllegalArgumentException("userId is required when audience=SINGLE_USER");
                }
                return userRepo.findById(req.getUserId())
                        .map(List::of)
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.getUserId()));
            default:
                throw new IllegalArgumentException("Unknown audience: " + req.getAudience());
        }
    }

    private String buildAudienceLabel(NotificationSendRequest req) {
        if ("ALL_USERS".equals(req.getAudience()))
            return "All users";
        if ("HUB".equals(req.getAudience()))
            return "Hub #" + req.getHubId();
        if ("SINGLE_USER".equals(req.getAudience()))
            return "Single user";
        return req.getAudience();
    }
}