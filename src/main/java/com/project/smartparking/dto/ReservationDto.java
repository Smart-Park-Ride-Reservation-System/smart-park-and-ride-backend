package com.project.smartparking.dto;

import com.project.smartparking.model.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    private Long reservationId;
    private UserSummaryDto user;
    private SlotSummaryDto slot;
    private Long hubId;
    private LocalDateTime createdAt;
    private String vehicleNo;
    private String vehicleType;
    private String status;
    private LocalDateTime bookingDate;
    private LocalDateTime scheduledIn;
    private LocalDateTime scheduledOut;
    private LocalDateTime actualIn;
    private LocalDateTime actualOut;
    private String qrToken;
    private BigDecimal reservationFee;

    public static ReservationDto from(Reservation r) {
        Long hubId = null;
        if (r.getSlot() != null && r.getSlot().getLot() != null && r.getSlot().getLot().getHub() != null) {
            hubId = r.getSlot().getLot().getHub().getHubId();
        }

        return new ReservationDto(
                r.getReservationId(),
                UserSummaryDto.from(r.getUser()),
                SlotSummaryDto.from(r.getSlot()),
                hubId,
                r.getCreatedAt(),
                r.getVehicleNo(),
                r.getVehicleType(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getBookingDate(),
                r.getScheduledIn(),
                r.getScheduledOut(),
                r.getActualIn(),
                r.getActualOut(),
                r.getQrToken(),
                r.getReservationFee()
        );
    }
}