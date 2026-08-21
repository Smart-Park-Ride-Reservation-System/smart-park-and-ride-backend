package com.project.smartparking.repository;

import com.project.smartparking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationReservationId(Long reservationId);

    List<Payment> findByPaymentStatus(Payment.PaymentStatus paymentStatus);

    // Hub-scoped: payments for reservations in a given hub
    @Query("SELECT p FROM Payment p WHERE p.reservation.slot.lot.hub.hubId = :hubId ORDER BY p.reservation.createdAt DESC")
    List<Payment> findByHubId(@Param("hubId") Long hubId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = :status AND p.paymentMethod IN :methods")
    BigDecimal sumAmountByStatusAndMethods(
            @Param("status") Payment.PaymentStatus status,
            @Param("methods") List<Payment.PaymentMethod> methods
    );

    // Hub-scoped revenue summary
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = :status AND p.paymentMethod IN :methods AND p.reservation.slot.lot.hub.hubId = :hubId")
    BigDecimal sumAmountByStatusAndMethodsAndHub(
            @Param("status") Payment.PaymentStatus status,
            @Param("methods") List<Payment.PaymentMethod> methods,
            @Param("hubId") Long hubId
    );
}