package com.project.smartparking.controller;

import com.project.smartparking.Config.AuthenticatedUser;
import com.project.smartparking.model.Payment;
import com.project.smartparking.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT')")
    public ResponseEntity<?> getAll(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        // HUB_ADMIN only sees payments for their assigned hub
        if ("HUB_ADMIN".equals(principal.role())) {
            if (principal.hubId() == null) {
                return ResponseEntity.status(403).body("Hub admin has no assigned hub");
            }
            return ResponseEntity.ok(paymentService.getByHub(principal.hubId()));
        }
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT')")
    public List<Payment> getByReservation(@PathVariable Long reservationId) {
        return paymentService.getByReservation(reservationId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT')")
    public ResponseEntity<?> create(@RequestBody Payment payment) {
        return ResponseEntity.ok(paymentService.create(payment));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(paymentService.updateStatus(id, status));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'HUB_ADMIN', 'ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Payment payment) {
        return ResponseEntity.ok(paymentService.update(id, payment));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'HUB_ADMIN', 'ADMIN')")
    public ResponseEntity<?> getRevenueSummary(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        // HUB_ADMIN only sees revenue for their hub
        if ("HUB_ADMIN".equals(principal.role())) {
            if (principal.hubId() == null) {
                return ResponseEntity.status(403).body("Hub admin has no assigned hub");
            }
            return ResponseEntity.ok(paymentService.getRevenueSummaryByHub(principal.hubId()));
        }
        return ResponseEntity.ok(paymentService.getRevenueSummary());
    }
}