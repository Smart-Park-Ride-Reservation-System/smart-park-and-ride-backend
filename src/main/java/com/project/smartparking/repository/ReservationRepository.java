package com.project.smartparking.repository;

import com.project.smartparking.model.Reservation;
import com.project.smartparking.model.Reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Fix 1: Traverses Reservation -> user -> userId
    List<Reservation> findByUser_UserId(Long userId);

    List<Reservation> findByScheduledInLessThanEqualAndReminderSentFalse(LocalDateTime threshold);

    Optional<Reservation> findByQrToken(String qrToken);

    // Fix 2: Changed String status -> ReservationStatus status
    List<Reservation> findByStatusAndScheduledInBeforeAndActualInIsNull(
            ReservationStatus status,
            LocalDateTime cutoffTime);

    // Fix 3: Changed String status -> ReservationStatus status
    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime cutoff);

    List<Reservation> findByScheduledInBetweenAndReminderSentFalse(
            LocalDateTime start,
            LocalDateTime end);

    // Fix 4: Changed Collection<String> statuses -> Collection<ReservationStatus> statuses
    List<Reservation> findBySlotSlotIdAndStatusIn(Long slotId, Collection<ReservationStatus> statuses);

    @Query("""
            SELECT DISTINCT r.slot.slotId FROM Reservation r
            WHERE r.slot.lot.hub.hubId = :hubId
              AND r.status IN (com.project.smartparking.model.Reservation.ReservationStatus.CONFIRMED, 
                               com.project.smartparking.model.Reservation.ReservationStatus.PENDING, 
                               com.project.smartparking.model.Reservation.ReservationStatus.CHECKED_IN)
              AND r.scheduledIn < :endTime
              AND COALESCE(r.actualOut, r.scheduledOut) > :startTime
            """)
    List<Long> findReservedSlotIdsInWindow(
            @Param("hubId") Long hubId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}