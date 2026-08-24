package com.project.smartparking.scheduler;

import com.project.smartparking.model.NotificationType;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.ReservationRepository;
import com.project.smartparking.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private ReservationRepository reservationRepo;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        
        LocalDateTime reminderThreshold = now.plusMinutes(15);

        List<Reservation> upcoming = reservationRepo
                .findByScheduledInLessThanEqualAndReminderSentFalse(reminderThreshold);

        for (Reservation r : upcoming) {
            if (r.getUser() == null || r.getUser().getUserId() == null) {
                continue;
            }

            Long userId = r.getUser().getUserId();
            String locationInfo = (r.getSlot() != null && r.getSlot().getLot() != null)
                    ? r.getSlot().getLot().getLotCode() + " - " + r.getSlot().getSlotCode()
                    : "assigned slot";

            String message = String.format(
                "Your reservation for slot (%s) starts soon at %s", 
                locationInfo, 
                r.getScheduledIn()
            );

            try {
                notificationService.notify(
                        userId,
                        "Reservation Reminder",
                        message,
                        NotificationType.SYSTEM,
                        true
                );

                r.setReminderSent(true);
                reservationRepo.save(r);
            } catch (Exception e) {
                System.err.println("Failed to send reminder for reservation ID: " + r.getReservationId() + " - " + e.getMessage());
            }
        }
    }
}