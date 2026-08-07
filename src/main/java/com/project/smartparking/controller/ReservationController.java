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
    public ResponseEntity<?> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PutMapping("/{id}/confirm-payment")
    public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirmPayment(id));
    }

    @PutMapping("/checkin")
    public ResponseEntity<?> checkInByCode(@RequestParam String code) {
        return ResponseEntity.ok(reservationService.checkInByQrToken(code));
    }

    @PutMapping("/{id}/checkin")
    public ResponseEntity<?> checkInById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.checkInById(id));
    }

    @PutMapping("/{id}/checkout")
    public ResponseEntity<?> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.checkOut(id));
    }

    @PutMapping("/{id}/paid")
    public ResponseEntity<?> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.markPaid(id));
    }

    @PutMapping("/gate-a/scan") // ENTRY — always check-in
    public ResponseEntity<?> gateAScan(@RequestParam String qrToken) {
        return ResponseEntity.ok(reservationService.checkInByQrToken(qrToken));
    }

    @PutMapping("/gate-b/scan") // EXIT — always check-out
    public ResponseEntity<?> gateBScan(@RequestParam String qrToken) {
        return ResponseEntity.ok(reservationService.checkOutByQrToken(qrToken));
    }
}