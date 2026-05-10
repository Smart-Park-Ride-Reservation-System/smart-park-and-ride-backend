package com.project.smartparking.controller;

import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:3000")
public class ReservationController {

    @Autowired
    private ReservationRepository reservationRepo;

    // GET all reservations (for dashboard)
    @GetMapping
    public List<Reservation> getAll() {
        return reservationRepo.findAll();
    }

    // CREATE reservation (user booking)
    @PostMapping
    public Reservation create(@RequestBody Reservation r) {
        r.setBookingDate(LocalDateTime.now());
        r.setStatus("PENDING");
        return reservationRepo.save(r);
    }

    // CHECK-IN (gatekeeper)
    @PutMapping("/{id}/checkin")
    public ResponseEntity<?> checkIn(@PathVariable Long id) {

        Reservation r = reservationRepo.findById(id)
                .orElseThrow();

        r.setActualIn(LocalDateTime.now());
        r.setStatus("CHECKED_IN");

        return ResponseEntity.ok(reservationRepo.save(r));
    }

    // CHECK-OUT (gatekeeper)
    @PutMapping("/{id}/checkout")
    public ResponseEntity<?> checkOut(@PathVariable Long id) {

        Reservation r = reservationRepo.findById(id)
                .orElseThrow();

        r.setActualOut(LocalDateTime.now());
        r.setStatus("COMPLETED");

        return ResponseEntity.ok(reservationRepo.save(r));
    }
}