package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_broadcasts")
@Data
@NoArgsConstructor
public class NotificationBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "display_type")
    private String displayType;

    @Column(name = "status")
    private String status;

    @Column(name = "audience")
    private String audience;

    @Column(name = "audience_label")
    private String audienceLabel;

    @Column(name = "recipient_count")
    private int recipientCount;

    @Column(name = "hub_id")
    private Long hubId;

    @Column(name = "single_user_id")
    private Long singleUserId;

    @Column(name = "sent_at")
    private Timestamp sentAt;

    @Column(name = "scheduled_for")
    private Timestamp scheduledFor;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "send_email")
    private boolean sendEmail;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Timestamp.valueOf(LocalDateTime.now());
    }
}