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
    private final String vehicleNo;
    private final String vehicleType;
    private final String reservationType;
    private final String status;
    private final LocalDateTime bookingDate;
    private final LocalDateTime scheduledIn;
    private final LocalDateTime actualIn;
    private final LocalDateTime actualOut;
    private final String qrToken;
    private final BigDecimal amount; 

    public ReservationResponse(Reservation r, BigDecimal amount) {
        this.reservationId = r.getReservationId();
        this.userId = r.getUserId();
        this.slotId = r.getSlotId();
        this.vehicleNo = r.getVehicleNo();
        this.vehicleType = r.getVehicleType();
        this.reservationType = r.getReservationType();
        this.status = r.getStatus();
        this.bookingDate = r.getBookingDate();
        this.scheduledIn = r.getScheduledIn();
        this.actualIn = r.getActualIn();
        this.actualOut = r.getActualOut();
        this.qrToken = r.getQrToken();
        this.amount = amount;
    }
}