package com.project.smartparking.repository;

import com.project.smartparking.model.NotificationBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import java.sql.Timestamp;
import java.util.List;

public interface NotificationBroadcastRepository extends JpaRepository<NotificationBroadcast, Long> {
    List<NotificationBroadcast> findAllByOrderByCreatedAtDesc();
    List<NotificationBroadcast> findByStatusAndScheduledForBefore(String status, Timestamp now);
    List<NotificationBroadcast> findByHubIdOrderByCreatedAtDesc(Long hubId);
}