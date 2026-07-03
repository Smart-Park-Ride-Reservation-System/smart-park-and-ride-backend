package com.project.smartparking.repository;

import com.project.smartparking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        List<Reservation> findByUserId(Long userId);

        List<Reservation> findBySlotId(Long slotId);

        List<Reservation> findByStatus(String status);

        List<Reservation> findByStatusAndScheduledInBeforeAndActualInIsNull(
                        String status,
                        LocalDateTime time);

        List<Reservation> findByStatusAndBookingDateBefore(String status, LocalDateTime cutoff);

        @Query("SELECT r FROM Reservation r WHERE r.slotId = :slotId AND r.status IN :statusList")
        List<Reservation> findBySlotIdAndStatusIn(
                        @Param("slotId") Long slotId,
                        @Param("statusList") List<String> statusList);

        @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
                        "FROM Reservation r WHERE r.slotId = :slotId " +
                        "AND r.scheduledIn = :scheduledIn AND r.status IN :statusList")
        boolean existsBySlotIdAndScheduledInAndStatusIn(
                        @Param("slotId") Long slotId,
                        @Param("scheduledIn") LocalDateTime scheduledIn,
                        @Param("statusList") List<String> statusList);

        Optional<Reservation> findByQrToken(String qrToken);

        @Query("SELECT r FROM Reservation r WHERE UPPER(r.qrToken) LIKE CONCAT('%', :suffix)")
        Optional<Reservation> findByQrTokenSuffix(@Param("suffix") String suffix);

}