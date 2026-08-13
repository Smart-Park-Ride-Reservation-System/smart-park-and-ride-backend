package com.project.smartparking.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
        Reservation reservation = findReservationByToken(qrToken);

        if (reservation == null) {
            return ScanResult.rejected("QR code not recognized");
        }

        return switch (gateType) {
            case ENTRY -> handleEntry(reservation, gateId);
            case EXIT -> handleExit(reservation, gateId);
        };
    }

    private ScanResult handleEntry(Reservation reservation, String gateId) {
        if (!"PENDING".equals(reservation.status())) {
            return ScanResult.rejected(
                "Reservation is not pending (current status: " + reservation.status() + ")");
        }

        jdbcTemplate.update(
            "UPDATE reservations SET status = 'CHECKED_IN', actual_in = ? WHERE reservation_id = ?",
            Timestamp.from(Instant.now()), reservation.id());

        openGate(gateId);
        setSlotReserved(reservation.slotId(), true);

        return ScanResult.success("Checked in — slot " + reservation.slotId(), reservation.id());
    }

    private ScanResult handleExit(Reservation reservation, String gateId) {
        if (!"CHECKED_IN".equals(reservation.status())) {
            return ScanResult.rejected(
                "Reservation is not checked in (current status: " + reservation.status() + ")");
        }

        jdbcTemplate.update(
            "UPDATE reservations SET status = 'COMPLETED', actual_out = ? WHERE reservation_id = ?",
            Timestamp.from(Instant.now()), reservation.id());

        openGate(gateId);
        setSlotReserved(reservation.slotId(), false);

        return ScanResult.success("Checked out — slot " + reservation.slotId(), reservation.id());
    }

    private Reservation findReservationByToken(String qrToken) {
        String sql = "SELECT reservation_id, slot_id, status FROM reservations WHERE qr_token = ?";
        List<Reservation> results = jdbcTemplate.query(sql,
            (rs, rowNum) -> new Reservation(
                rs.getLong("reservation_id"),
                rs.getLong("slot_id"),
                rs.getString("status")),
            qrToken);

        return results.isEmpty() ? null : results.get(0);
    }

    private void openGate(String gateId) {
        DatabaseReference gateRef = FirebaseDatabase.getInstance()
            .getReference("gates/" + gateId + "/open");
        gateRef.setValueAsync(true);
    }

    private void setSlotReserved(Long slotId, boolean reserved) {
        DatabaseReference slotRef = FirebaseDatabase.getInstance()
            .getReference("slots/" + slotId);

        Map<String, Object> update = new HashMap<>();
        update.put("reserved", reserved);
        update.put("updatedAt", Instant.now().toString());

        slotRef.updateChildrenAsync(update);
    }

    public enum GateType { ENTRY, EXIT }

    public record Reservation(Long id, Long slotId, String status) {}

    public record ScanResult(boolean ok, String message, Long reservationId) {
        public static ScanResult success(String message, Long reservationId) {
            return new ScanResult(true, message, reservationId);
        }
        public static ScanResult rejected(String message) {
            return new ScanResult(false, message, null);
        }
    }
}