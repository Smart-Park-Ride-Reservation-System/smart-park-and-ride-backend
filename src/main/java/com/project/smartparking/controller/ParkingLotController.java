package com.project.smartparking.controller;

import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.service.ParkingLotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
public class ParkingLotController {

    @Autowired
    private ParkingLotService parkingLotService;

    // Open read access for all roles including commuters
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT', 'COMMUTER')")
    @GetMapping
    public ResponseEntity<List<ParkingLot>> getAll() {
        return ResponseEntity.ok(parkingLotService.getAllParkingLots());
    }

    // Used by Next.js slot reservation screen
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT', 'COMMUTER')")
    @GetMapping("/by-hub/{hubId}")
    public ResponseEntity<List<ParkingLot>> getByHub(@PathVariable Long hubId) {
        return ResponseEntity.ok(parkingLotService.getParkingLotsByHub(hubId));
    }

    // CREATE LOT: Admin only
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @PostMapping
    public ResponseEntity<ParkingLot> create(@RequestBody ParkingLot parkingLot) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parkingLotService.createParkingLot(parkingLot));
    }

    // UPDATE LOT: Admin only
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ParkingLot> update(
            @PathVariable Long id,
            @RequestBody ParkingLot parkingLot) {
        return ResponseEntity.ok(parkingLotService.updateParkingLot(id, parkingLot));
    }

    // DELETE LOT: Admin only
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parkingLotService.deleteParkingLotById(id);
        return ResponseEntity.noContent().build();
    }
}