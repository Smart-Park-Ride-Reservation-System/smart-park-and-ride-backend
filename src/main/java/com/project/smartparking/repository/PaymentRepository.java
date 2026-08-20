package com.project.smartparking.repository;

import com.project.smartparking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Returns Optional if a reservation has at most one payment record
    Optional<Payment> findByReservationReservationId(Long reservationId);

    List<Payment> findByPaymentStatus(Payment.PaymentStatus paymentStatus);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = :status AND p.paymentMethod IN :methods")
    BigDecimal sumAmountByStatusAndMethods(
            @Param("status") Payment.PaymentStatus status,
            @Param("methods") List<Payment.PaymentMethod> methods
    );
}