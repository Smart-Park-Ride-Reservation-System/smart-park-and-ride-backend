package com.project.smartparking.controller;

import com.project.smartparking.model.Payment;
import com.project.smartparking.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public List<Payment> getAll() {
        return paymentService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/reservation/{reservationId}")
    public List<Payment> getByReservation(@PathVariable Long reservationId) {
        return paymentService.getByReservation(reservationId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Payment payment) {
        return ResponseEntity.ok(paymentService.create(payment));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(paymentService.updateStatus(id, status));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Payment payment) {
        return ResponseEntity.ok(paymentService.update(id, payment));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'ADMIN')")
    public ResponseEntity<?> getRevenueSummary() {
        return ResponseEntity.ok(paymentService.getRevenueSummary());
    }
}