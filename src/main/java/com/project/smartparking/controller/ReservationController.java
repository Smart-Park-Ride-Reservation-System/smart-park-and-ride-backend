package com.project.smartparking.controller;

import com.project.smartparking.model.Reservation;
import com.project.smartparking.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public List<Reservation> getAll() {
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
    @PreAuthorize("@securityAccess.isOwner(authentication, #r.userId)")
    public ResponseEntity<?> create(@RequestBody Reservation r) {
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

    // Commuter pays online before arriving at the hub.
    @PutMapping("/{id}/confirm-payment")
    @PreAuthorize("@securityAccess.canAccessReservation(authentication, #id)")
    public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirmPayment(id));
    }

    // Staff marks a reservation paid at the gate (e.g. cash).
    @PutMapping("/{id}/paid")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.markPaid(id));
    }

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
}