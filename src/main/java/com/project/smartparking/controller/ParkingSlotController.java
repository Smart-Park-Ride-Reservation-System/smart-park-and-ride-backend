package com.project.smartparking.controller;

import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.service.ParkingSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = "http://localhost:3000")

public class ParkingSlotController {

    @Autowired
    private ParkingSlotService slotService;

    @GetMapping
    public ResponseEntity<List<ParkingSlot>> getAll() {
        return ResponseEntity.ok(slotService.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ParkingSlot>> search(@RequestParam Long hubId) {
        return ResponseEntity.ok(slotService.findAvailableByHub(hubId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ParkingSlot> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        ParkingSlot.SlotStatus slotStatus =
                ParkingSlot.SlotStatus.valueOf(status.toUpperCase());

        return ResponseEntity.ok(slotService.updateStatus(id, slotStatus));
    }

    @GetMapping("/hubs")
    public ResponseEntity<List<Hub>> getAllHubs() {
        return ResponseEntity.ok(slotService.getAllHubs());
    }
}