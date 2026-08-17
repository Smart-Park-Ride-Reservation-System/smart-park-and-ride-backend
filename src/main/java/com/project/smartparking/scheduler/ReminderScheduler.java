package com.project.smartparking.scheduler;

import com.project.smartparking.model.NotificationType;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.ReservationRepository;
import com.project.smartparking.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private ReservationRepository reservationRepo;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedRate = 60000)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusMinutes(5);
        LocalDateTime windowEnd = now.plusMinutes(6);

        List<Reservation> upcoming = reservationRepo
                .findByScheduledInBetweenAndReminderSentFalse(windowStart, windowEnd);

        for (Reservation r : upcoming) {
            notificationService.notify(
                    r.getUserId(),
                    "Reservation reminder",
                    "Your reservation starts in 5 minutes at " + r.getScheduledIn(),
                    NotificationType.SYSTEM,
                    true);
            r.setReminderSent(true);
            reservationRepo.save(r);
        }
    }
}
