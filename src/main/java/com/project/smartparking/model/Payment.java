package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payment_status")
    private String paymentStatus; // PENDING, SUCCESS, FAILED, REFUNDED

    @Column(name = "payment_method")
    private String paymentMethod; // CARD, PAYHERE, CASH

    @PrePersist
    protected void onCreate() {
        this.paymentDate = LocalDateTime.now();
        if (this.paymentStatus == null) this.paymentStatus = "PENDING";
    }
}