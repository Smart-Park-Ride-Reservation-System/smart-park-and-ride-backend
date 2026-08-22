package com.project.smartparking.service;

import com.project.smartparking.dto.NotificationSendRequest;
import com.project.smartparking.model.NotificationBroadcast;
import com.project.smartparking.model.NotificationType;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.NotificationBroadcastRepository;
import com.project.smartparking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import com.project.smartparking.model.User;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional
public class NotificationBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastService.class);

    @Autowired
    private NotificationBroadcastRepository broadcastRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<NotificationBroadcast> getAll() {
        return broadcastRepo.findAllByOrderByCreatedAtDesc();
    }

    public NotificationBroadcast send(NotificationSendRequest req, User caller) {
        enforceAudienceRestrictions(req, caller);
        log.info("Broadcast requested: title='{}' audience={} hubId={} scheduledFor={}",
                req.getTitle(), req.getAudience(), req.getHubId(), req.getScheduledFor());

        NotificationBroadcast broadcast = new NotificationBroadcast();
        broadcast.setTitle(req.getTitle());
        broadcast.setMessage(req.getMessage());
        broadcast.setDisplayType(req.getType());
        broadcast.setAudience(req.getAudience());
        broadcast.setHubId(req.getHubId());
        broadcast.setSingleUserId(req.getUserId());
        broadcast.setSendEmail(req.isSendEmail());
        broadcast.setAudienceLabel(buildAudienceLabel(req));

        if (req.getScheduledFor() != null && !req.getScheduledFor().isBlank()) {
            try {
                Timestamp scheduled = Timestamp.valueOf(LocalDateTime.parse(req.getScheduledFor()));
                broadcast.setScheduledFor(scheduled);
                broadcast.setStatus("SCHEDULED");
                broadcast.setRecipientCount(0);
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
                return broadcastRepo.save(broadcast);
            }

            int count = notificationService.notifyMany(
                    recipients, req.getTitle(), req.getMessage(), NotificationType.SYSTEM, req.isSendEmail());

            broadcast.setStatus("SENT");
            broadcast.setRecipientCount(count);
            broadcast.setSentAt(Timestamp.valueOf(LocalDateTime.now()));
            return broadcastRepo.save(broadcast);

        } catch (Exception e) {
            log.error("Broadcast send failed", e);
            broadcast.setStatus("FAILED");
            broadcast.setRecipientCount(0);
            broadcast.setFailureReason(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            return broadcastRepo.save(broadcast);
        }
    }

    // NotificationBroadcastService
    @Transactional(readOnly = true)
    public List<NotificationBroadcast> getAllByHub(Long hubId) {
        return broadcastRepo.findByHubIdOrderByCreatedAtDesc(hubId);
    }

    @Transactional(readOnly = true)
    public NotificationBroadcast getById(Long id) {
        return broadcastRepo.findById(id).orElse(null);
    }

    // Service
    public NotificationBroadcast resend(Long broadcastId, User caller) {
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

        return send(req, caller);
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
        if (req.getAudience() == null) {
            throw new IllegalArgumentException("Audience type cannot be null");
        }

        switch (req.getAudience().toUpperCase()) {
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
        if (req.getAudience() == null)
            return "Unknown";

        switch (req.getAudience().toUpperCase()) {
            case "ALL_USERS":
                return "All users";
            case "HUB":
                return "Hub #" + req.getHubId();
            case "SINGLE_USER":
                return "Single user #" + req.getUserId();
            default:
                return req.getAudience();
        }
    }

    private void enforceAudienceRestrictions(NotificationSendRequest req, User caller) {
        if (caller.getRole() != User.Role.HUB_ADMIN)
            return; // ADMIN unrestricted

        if (caller.getHub() == null) {
            throw new AccessDeniedException("Hub admin has no assigned hub");
        }
        Long ownHubId = caller.getHub().getHubId();

        switch (req.getAudience() == null ? "" : req.getAudience().toUpperCase()) {
            case "HUB":
                req.setHubId(ownHubId); // never trust client-supplied hubId for this role
                break;
            case "SINGLE_USER":
                User target = userRepo.findById(req.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.getUserId()));
                if ((target.getRole() != User.Role.ATTENDANT && target.getRole() != User.Role.COMMUTER)
                        || target.getHub() == null
                        || !target.getHub().getHubId().equals(ownHubId)) {
                    throw new AccessDeniedException("Hub admin can only message their own hub's attendants and commuters");
                }
                break;
            default:
                throw new AccessDeniedException("Hub admin cannot use audience=" + req.getAudience());
        }
    }
}