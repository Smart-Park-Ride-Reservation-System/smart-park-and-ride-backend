package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    // Direct mapping to ParkingSlot (Hub is accessible via slot.getLot().getHub())
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private ParkingSlot slot;

    // Fixed: User entity association instead of a raw Long ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vehicle_no", length = 20)
    private String vehicleNo;

    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    @Column(name = "scheduled_in", nullable = false)
    private LocalDateTime scheduledIn;

    @Column(name = "scheduled_out", nullable = false)
    private LocalDateTime scheduledOut;

    @Column(name = "actual_in")
    private LocalDateTime actualIn;

    @Column(name = "actual_out")
    private LocalDateTime actualOut;

    @Column(name = "qr_token", unique = true, length = 255)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private ReservationStatus status;

    // Fixed: Changed Double to BigDecimal for financial precision
    @Column(name = "reservation_fee", precision = 10, scale = 2)
    private BigDecimal reservationFee;

    @Builder.Default
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.bookingDate == null) {
            this.bookingDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReservationStatus.PENDING;
        }
    }

    public enum ReservationStatus {
        PENDING, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, EXPIRED
    }
}