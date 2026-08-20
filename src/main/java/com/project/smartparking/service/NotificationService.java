package com.project.smartparking.service;

import com.project.smartparking.model.Notification;
import com.project.smartparking.model.NotificationType;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.NotificationRepository;
import com.project.smartparking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepo;

    public Notification notify(Long userId, String title, String message, NotificationType type, boolean sendEmail) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type != null ? type.name() : null);
        notification.setRead(false);

        Notification saved = notificationRepo.save(notification);

        if (sendEmail) {
            sendEmailSafely(userId, title, message);
        }

        return saved;
    }

    public int notifyMany(List<User> users, String title, String message, NotificationType type, boolean sendEmail) {
        if (users == null || users.isEmpty()) {
            return 0;
        }

        List<Notification> notifications = new ArrayList<>();
        String typeName = type != null ? type.name() : null;

        for (User user : users) {
            Notification notification = new Notification();
            notification.setUserId(user.getUserId());
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(typeName);
            notification.setRead(false);
            notifications.add(notification);
        }

        // Batch save to improve performance
        List<Notification> savedNotifications = notificationRepo.saveAll(notifications);

        if (sendEmail) {
            for (User user : users) {
                if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    sendEmailSafely(user.getEmail(), title, message);
                }
            }
        }

        return savedNotifications.size();
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepo.findByUserIdAndReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + notificationId));
        notification.setRead(true);
        return notificationRepo.save(notification);
    }

    private void sendEmailSafely(Long userId, String title, String message) {
        userRepo.findById(userId).ifPresent(user -> {
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                sendEmailSafely(user.getEmail(), title, message);
            }
        });
    }

    private void sendEmailSafely(String email, String title, String message) {
        try {
            emailService.send(email, title, message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", email, e.getMessage());
        }
    }
}