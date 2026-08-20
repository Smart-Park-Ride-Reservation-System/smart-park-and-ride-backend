package com.project.smartparking.controller;

import com.project.smartparking.dto.ParkingSlotDto;
import com.project.smartparking.dto.SlotGenerateRequest;
import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.service.ParkingSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/slots")
public class ParkingSlotController {

    @Autowired
    private ParkingSlotService slotService;

    // slots
    @GetMapping
    public ResponseEntity<List<ParkingSlotDto>> getAll() {
        List<ParkingSlotDto> slots = slotService.getAllWithLotAndHub()
                .stream()
                .map(ParkingSlotDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(slots);
    }

    // overlaps
    @GetMapping("/search")
    public ResponseEntity<List<ParkingSlotDto>> search(
            @RequestParam Long hubId,
            @RequestParam Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        LocalDateTime start = (startTime != null) ? startTime : LocalDateTime.now();
        LocalDateTime end = (endTime != null) ? endTime : start.plusHours(2);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        List<ParkingSlotDto> slots = slotService.findByLot(hubId, lotId, start, end)
                .stream()
                .map(ParkingSlotDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(slots);
    }

    @GetMapping("/hubs")
    public ResponseEntity<List<Hub>> getAllHubs() {
        return ResponseEntity.ok(slotService.getAllHubs());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ParkingSlot> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ParkingSlot.SlotStatus slotStatus = ParkingSlot.SlotStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(slotService.updateStatus(id, slotStatus));
    }

    // RESTRICTED: Only admins can generate grid structures
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<Void> generateSlots(@RequestBody SlotGenerateRequest request) {
        slotService.generateSlots(request.getLotId(), request.getGrid());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // RESTRICTED: Only admins can delete slots
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}