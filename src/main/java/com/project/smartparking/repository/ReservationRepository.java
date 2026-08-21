package com.project.smartparking.repository;

import com.project.smartparking.model.Reservation;
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

      List<Reservation> findByUser_UserId(Long userId);

      List<Reservation> findByScheduledInLessThanEqualAndReminderSentFalse(LocalDateTime threshold);

      Optional<Reservation> findByQrToken(String qrToken);

      List<Reservation> findByStatusAndScheduledInBeforeAndActualInIsNull(
                  Reservation.ReservationStatus status,
                  LocalDateTime cutoffTime);

      List<Reservation> findByStatusAndCreatedAtBefore(Reservation.ReservationStatus status, LocalDateTime cutoff);

      List<Reservation> findByScheduledInBetweenAndReminderSentFalse(
                  LocalDateTime start,
                  LocalDateTime end);

      List<Reservation> findBySlotSlotIdAndStatusIn(Long slotId, Collection<Reservation.ReservationStatus> statuses);

      @Query("""
                      SELECT DISTINCT r.slot.slotId
                  FROM Reservation r
                  WHERE r.slot.lot.hub.hubId = :hubId
                    AND (
                      COALESCE(r.actualIn, r.scheduledIn) < :endTime
                      AND (
                        r.status = com.project.smartparking.model.Reservation.ReservationStatus.CHECKED_IN
                        OR COALESCE(r.actualOut, r.scheduledOut) > :startTime
                      )
                    )
                    AND (
                      r.status = com.project.smartparking.model.Reservation.ReservationStatus.CHECKED_IN
                      OR (
                          r.status = com.project.smartparking.model.Reservation.ReservationStatus.PENDING
                          AND r.createdAt > :pendingCutoff
                      )
                      OR (
                          r.status = com.project.smartparking.model.Reservation.ReservationStatus.CONFIRMED
                          AND r.scheduledIn > :noShowCutoff
                      )
                    )
                      """)
      List<Long> findReservedSlotIdsInWindow(
                  @Param("hubId") Long hubId,
                  @Param("startTime") LocalDateTime startTime,
                  @Param("endTime") LocalDateTime endTime,
                  @Param("pendingCutoff") LocalDateTime pendingCutoff,
                  @Param("noShowCutoff") LocalDateTime noShowCutoff);
}