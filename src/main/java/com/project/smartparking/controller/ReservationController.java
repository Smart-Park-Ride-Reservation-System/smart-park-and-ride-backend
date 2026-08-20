package com.project.smartparking.controller;

import com.project.smartparking.dto.ReservationDto;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public List<ReservationDto> getAll() {
        return reservationService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityAccess.canAccessReservation(authentication, #id)")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@securityAccess.isOwner(authentication, #userId)")
    public List<Reservation> getByUser(@PathVariable Long userId) {
        return reservationService.getByUser(userId);
    }

    @PostMapping
    @PreAuthorize("@securityAccess.isOwner(authentication, #r.user.userId)")
    public ResponseEntity<?> create(
            @RequestBody @org.springframework.security.core.parameters.P("r") Reservation r) {
        return ResponseEntity.ok(reservationService.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityAccess.canAccessReservation(authentication, #id)")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Reservation updated) {
        return ResponseEntity.ok(reservationService.update(id, updated));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("@securityAccess.canAccessReservation(authentication, #id)")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.ok("Cancelled successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PutMapping("/{id}/confirm-payment")
    @PreAuthorize("@securityAccess.canAccessReservation(authentication, #id)")
    public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirmPayment(id));
    }

    @PutMapping("/{id}/paid")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.markPaid(id));
    }

    // --- MANUAL OVERRIDE ENDPOINT ---
    @PutMapping("/slots/{slotId}/status")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<ParkingSlot> setSlotStatusManually(
            @PathVariable Long slotId,
            @RequestParam ParkingSlot.SlotStatus status) {
        return ResponseEntity.ok(reservationService.setSlotStatusManually(slotId, status));
    }

    // --- QR CHECK-IN / CHECK-OUT ENDPOINTS ---
    @PutMapping("/checkin")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> checkInByCode(@RequestParam String code) {
        return ResponseEntity.ok(reservationService.checkInByQrToken(code));
    }

    @PutMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> checkInById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.checkInById(id));
    }

    @PutMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.checkOut(id));
    }

    @PutMapping("/gate-a/scan")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> gateAScan(@RequestParam String qrToken) {
        return ResponseEntity.ok(reservationService.checkInByQrToken(qrToken));
    }

    @PutMapping("/gate-b/scan")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> gateBScan(@RequestParam String qrToken) {
        return ResponseEntity.ok(reservationService.checkOutByQrToken(qrToken));
    }

    @PutMapping("/gate/scan")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> gateScan(@RequestParam String qrToken) {
        try {
            Reservation r = reservationService.processQrScan(qrToken);
            String action = "COMPLETED".equals(r.getStatus().name()) ? "Checked out" : "Checked in";

            Long slotId = (r.getSlot() != null) ? r.getSlot().getSlotId() : null;

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", action + " — slot #" + slotId,
                    "reservationId", r.getReservationId()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "ok", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Scan rejected",
                    "reservationId", null));
        }
    }
}