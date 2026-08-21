package com.project.smartparking.dto;

import com.project.smartparking.model.Reservation;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ReservationResponse {
    private final Long reservationId;
    private final Long userId;
    private final Long slotId;
    private final String slotCode; // Added: e.g. "A01"
    private final String lotCode; // Added: e.g. "L1"
    private final String hubName; // Added: e.g. "Central Hub"
    private final String vehicleNo;
    private final String vehicleType;
    private final String status;
    private final LocalDateTime bookingDate;
    private final LocalDateTime scheduledIn;
    private final LocalDateTime scheduledOut; // Added: missing scheduled end time
    private final LocalDateTime actualIn;
    private final LocalDateTime actualOut;
    private final String qrToken;
    private final BigDecimal amount;

    public ReservationResponse(Reservation r, BigDecimal amount) {
        this.reservationId = r.getReservationId();

        // Safe navigation for User object
        this.userId = (r.getUser() != null) ? r.getUser().getUserId() : null;

        // Safe navigation for Slot entity graph (Slot -> Lot -> Hub)
        if (r.getSlot() != null) {
            this.slotId = r.getSlot().getSlotId();
            this.slotCode = r.getSlot().getSlotCode();

            if (r.getSlot().getLot() != null) {
                this.lotCode = r.getSlot().getLot().getLotCode();
                this.hubName = (r.getSlot().getLot().getHub() != null)
                        ? r.getSlot().getLot().getHub().getName()
                        : null;
            } else {
                this.lotCode = null;
                this.hubName = null;
            }
        } else {
            this.slotId = null;
            this.slotCode = null;
            this.lotCode = null;
            this.hubName = null;
        }

        this.vehicleNo = r.getVehicleNo();
        this.vehicleType = r.getVehicleType();
        this.status = (r.getStatus() != null) ? r.getStatus().toString() : null;
        this.bookingDate = r.getBookingDate();
        this.scheduledIn = r.getScheduledIn();
        this.scheduledOut = r.getScheduledOut();
        this.actualIn = r.getActualIn();
        this.actualOut = r.getActualOut();
        this.qrToken = r.getQrToken();
        this.amount = amount != null ? amount : r.getReservationFee();
    }
}