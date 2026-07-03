package com.project.smartparking.controller;

import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.model.ParkingLot.LotStatus;
import com.project.smartparking.model.ParkingLot.LotType;
import com.project.smartparking.service.ParkingLotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
@CrossOrigin(origins = "http://localhost:3000")
public class ParkingLotController {

    @Autowired
    private ParkingLotService parkingLotService;

    @GetMapping
    public ResponseEntity<List<ParkingLot>> getAll() {
        return ResponseEntity.ok(parkingLotService.getAllParkingLots());
    }

    @GetMapping("/by-hub/{hubId}")
    public ResponseEntity<List<ParkingLot>> getByHub(@PathVariable Long hubId) {
        return ResponseEntity.ok(parkingLotService.getParkingLotsByHub(hubId));
    }

    @PostMapping
    public ResponseEntity<ParkingLot> create(@RequestBody ParkingLot parkingLot) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parkingLotService.createParkingLot(parkingLot));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingLot> update(
            @PathVariable Long id,
            @RequestBody ParkingLot parkingLot) {
        return ResponseEntity.ok(parkingLotService.updateParkingLot(id, parkingLot));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parkingLotService.deleteParkingLotById(id);
        return ResponseEntity.noContent().build();
    }
}