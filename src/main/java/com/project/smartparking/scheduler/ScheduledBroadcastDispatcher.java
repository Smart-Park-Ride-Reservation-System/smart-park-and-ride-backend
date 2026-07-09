package com.project.smartparking.scheduler;

import com.project.smartparking.model.NotificationBroadcast;
import com.project.smartparking.repository.NotificationBroadcastRepository;
import com.project.smartparking.service.NotificationBroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ScheduledBroadcastDispatcher {

    @Autowired
    private NotificationBroadcastRepository broadcastRepo;
    @Autowired
    private NotificationBroadcastService broadcastService;

    @Scheduled(fixedRate = 60000)
    public void dispatchDueBroadcasts() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        List<NotificationBroadcast> due = broadcastRepo.findByStatusAndScheduledForBefore("SCHEDULED", now);

        for (NotificationBroadcast b : due) {
            broadcastService.dispatchScheduled(b);
        }
    }
}
