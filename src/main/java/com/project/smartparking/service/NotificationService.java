package com.project.smartparking.service;

import com.project.smartparking.model.Notification;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.NotificationRepository;
import com.project.smartparking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    public static final String EMAIL = "EMAIL";
    public static final String SMS = "SMS";
    public static final String SYSTEM = "SYSTEM";

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepo;

    public Notification notify(Long userId, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setRead(false);

        Notification saved = notificationRepo.save(notification);

        if (EMAIL.equals(type)) {
            User user = userRepo.findById(userId).orElseThrow();
            emailService.send(user.getEmail(), title, message);
        }

        return saved;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        notification.setRead(true);
        return notificationRepo.save(notification);
    }
}