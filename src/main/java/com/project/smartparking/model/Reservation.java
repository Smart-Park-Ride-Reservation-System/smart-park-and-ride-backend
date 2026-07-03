package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "vehicle_no")
    private String vehicleNo;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    @Column(name = "scheduled_in")
    private LocalDateTime scheduledIn;

    @Column(name = "actual_in")
    private LocalDateTime actualIn;

    @Column(name = "actual_out")
    private LocalDateTime actualOut;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Column(name = "status")
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED, EXPIRED

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "reservation_type")
    private String reservationType;

    @Column(name = "reservation_fee")
    private Double reservationFee;

    @PrePersist
    protected void onCreate() {
        this.bookingDate = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }
}