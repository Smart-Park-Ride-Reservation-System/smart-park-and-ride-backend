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

    // Null = walk-in (no commuter account). Populated = commuter's own booking.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // Which attendant created this — set only for walk-ins/manual entries, null for self-booked
    @Column(name = "created_by_attendant_id")
    private Long createdByAttendantId;

    @Column(name = "vehicle_no", length = 20)
    private String vehicleNo;

    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    // Always required — the one timestamp everything hinges on (booking + check-in reference)
    @Column(name = "scheduled_in", nullable = false)
    private LocalDateTime scheduledIn;

    // No longer required — policy is check-in-time-only booking, no promised checkout time
    @Column(name = "scheduled_out")
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
        PENDING,      // commuter booked, not yet check-in time / not yet confirmed
        CONFIRMED,    // commuter booking confirmed, awaiting check-in
        CHECKED_IN,   // vehicle physically in the slot (commuter checked in, or walk-in assigned)
        COMPLETED,    // checked out, fee calculated
        CANCELLED,    // commuter cancelled before check-in
        EXPIRED,      // commuter no-show — passed grace period without check-in
        PAID          // fee settled (if you track payment separately from completion)
    }
}