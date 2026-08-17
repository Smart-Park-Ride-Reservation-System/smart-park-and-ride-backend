package com.project.smartparking.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.smartparking.model.Reservation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GateScanService {

    private final JdbcTemplate jdbcTemplate;

    public GateScanService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ScanResult handleScan(String qrToken, String gateId, GateType gateType) {
        ReservationData reservation = findReservationByToken(qrToken);

        if (reservation == null) {
            return ScanResult.rejected("QR code not recognized");
        }

        return switch (gateType) {
            case ENTRY -> handleEntry(reservation, gateId);
            case EXIT -> handleExit(reservation, gateId);
        };
    }

    private ScanResult handleEntry(ReservationData reservation, String gateId) {
        if (!"CONFIRMED".equalsIgnoreCase(reservation.status())) {
            return ScanResult.rejected("Reservation is not confirmed");
        }

        jdbcTemplate.update(
                "UPDATE reservations SET status = 'CHECKED_IN', actual_in = ? WHERE reservation_id = ?",
                Timestamp.from(Instant.now()), reservation.id());

        openDoor("in");
        syncSlotReservation(reservation.slotId(), true);

        return ScanResult.success("Checked in — slot #" + reservation.slotId(), reservation.id());
    }

    private ScanResult handleExit(ReservationData reservation, String gateId) {
        if (!"CHECKED_IN".equalsIgnoreCase(reservation.status())) {
            return ScanResult.rejected("Reservation is not checked in");
        }

        jdbcTemplate.update(
                "UPDATE reservations SET status = 'COMPLETED', actual_out = ? WHERE reservation_id = ?",
                Timestamp.from(Instant.now()), reservation.id());

        openDoor("out");
        syncSlotReservation(reservation.slotId(), false);

        return ScanResult.success("Checked out — slot #" + reservation.slotId(), reservation.id());
    }

    private ReservationData findReservationByToken(String qrToken) {
        String sql = "SELECT reservation_id, slot_id, status FROM reservations WHERE qr_token = ?";
        List<ReservationData> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> new ReservationData(
                        rs.getLong("reservation_id"),
                        rs.getLong("slot_id"),
                        rs.getString("status")),
                qrToken);

        return results.isEmpty() ? null : results.get(0);
    }

    private void openDoor(String doorType) {
        String path = doorType.equalsIgnoreCase("in") ? "in" : "out";
        FirebaseDatabase.getInstance()
                .getReference("door")
                .child(path)
                .setValueAsync("open");
    }

    private void syncSlotReservation(Long slotId, boolean reserved) {
        String sql = "SELECT zone, slot_number FROM parking_slots WHERE slot_id = ?";
        jdbcTemplate.query(sql, rs -> {
            String zone = rs.getString("zone");
            String slotNum = rs.getString("slot_number");

            DatabaseReference slotRef = FirebaseDatabase.getInstance()
                    .getReference("sensor/parking/" + zone + "/" + slotNum);

            Map<String, Object> update = new HashMap<>();
            update.put("reserved", reserved);
            update.put("updatedAt", Instant.now().toString());

            slotRef.updateChildrenAsync(update);
        }, slotId);
    }

    public enum GateType {
        ENTRY, EXIT
    }

    public record ReservationData(Long id, Long slotId, String status) {}

    public record ScanResult(boolean ok, String message, Long reservationId) {
        public static ScanResult success(String message, Long reservationId) {
            return new ScanResult(true, message, reservationId);
        }

        public static ScanResult rejected(String message) {
            return new ScanResult(false, message, null);
        }
    }
}