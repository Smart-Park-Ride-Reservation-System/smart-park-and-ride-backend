package com.project.smartparking.service;

import com.project.smartparking.model.Notification;
import com.project.smartparking.model.NotificationType;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.NotificationRepository;
import com.project.smartparking.service.EmailService;
import com.project.smartparking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

/**
 * Single entry point for all notification delivery in the app.
 *
 * Every feature - reservation confirmations, reminders, admin
 * broadcasts, future profile events - calls notify() here instead of
 * writing to the notifications table or calling EmailService directly.
 * This is what lets delivery rules change in one place instead of N.
 *
 * Current rules:
 *   - EMAIL  -> always saved as a DB record AND actually emailed.
 *   - SYSTEM -> saved as a DB record only (shows in the in-app bell).
 *   - SMS    -> reserved for future use, currently behaves like SYSTEM.
 */
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private EmailService emailService;  

    @Autowired
    private UserRepository userRepo;

    public Notification notify(Long userId, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type != null ? type.name() : null);
        notification.setRead(false);

        Notification saved = notificationRepo.save(notification);

        if (type == NotificationType.EMAIL) {
            userRepo.findById(userId).ifPresent(user -> {
                if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    emailService.send(user.getEmail(), title, message);
                }
            });
        }

        return saved;
    }

    /**
     * Bulk variant for admin broadcasts - notifies many users at once
     * under the same title/message/type. Returns how many DB records
     * were written (not how many emails succeeded).
     */
    public int notifyMany(List<User> users, String title, String message, NotificationType type) {
        int count = 0;
        for (User user : users) {
            notify(user.getUserId(), title, message, type);
            count++;
        }
        return count;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepo.findByUserIdAndReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        notification.setRead(true);
        return notificationRepo.save(notification);
    }
}
