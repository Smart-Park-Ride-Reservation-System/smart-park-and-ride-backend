package com.project.smartparking.controller;

import com.project.smartparking.dto.SlotGenerateRequest;
import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.service.ParkingSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class ParkingSlotController {

    @Autowired
    private ParkingSlotService slotService;

    // Open read operations to COMMUTER as well as management roles
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT', 'COMMUTER')")
    @GetMapping
    public ResponseEntity<List<ParkingSlot>> getAll() {
        return ResponseEntity.ok(slotService.getAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT', 'COMMUTER')")
    @GetMapping("/search")
    public ResponseEntity<List<ParkingSlot>> search(
            @RequestParam Long hubId,
            @RequestParam Long lotId) {
        return ResponseEntity.ok(slotService.findByLot(hubId, lotId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT', 'COMMUTER')")
    @GetMapping("/hubs")
    public ResponseEntity<List<Hub>> getAllHubs() {
        return ResponseEntity.ok(slotService.getAllHubs());
    }

    // RESTRICTED: Only staff can change slot statuses
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ParkingSlot> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ParkingSlot.SlotStatus slotStatus = ParkingSlot.SlotStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(slotService.updateStatus(id, slotStatus));
    }

    // RESTRICTED: Only admins can generate grid structures
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<Void> generateSlots(@RequestBody SlotGenerateRequest request) {
        slotService.generateSlots(request.getLotId(), request.getGrid());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // RESTRICTED: Only admins can delete slots
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}