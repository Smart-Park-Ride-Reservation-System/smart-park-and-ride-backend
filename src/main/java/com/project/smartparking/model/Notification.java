package com.project.smartparking.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notification_id;

    @Column(name = "user_id")
    private Long user_id;

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at")
    private Timestamp created_at;

    @Column(name = "is_read")
    private boolean is_read;
     // tinyint(1) maps to boolean in Java
    @Column(name = "notification_type")
    private String notification_type; // EMAIL, SMS, SYSTEM

     @PrePersist
    protected void onCreate() {
        this.created_at = Timestamp.valueOf(LocalDateTime.now());
    }
}