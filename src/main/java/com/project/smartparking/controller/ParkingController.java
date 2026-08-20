package com.project.smartparking.controller;

import com.project.smartparking.service.ParkingSlotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/parking")
public class ParkingController {

    private final ParkingSlotService parkingSlotService;

    public ParkingController(ParkingSlotService parkingSlotService) {
        this.parkingSlotService = parkingSlotService;
    }

    /**
     * Endpoint to open/close entry or exit barriers manually.
     * Example: POST /api/v1/parking/hubs/1/lots/101/doors/in?action=open
     */
    @PostMapping("/hubs/{hubId}/lots/{lotId}/doors/{doorType}")
    public ResponseEntity<?> controlDoor(
            @PathVariable int hubId,
            @PathVariable int lotId,
            @PathVariable String doorType,
            @RequestParam(defaultValue = "open") String action) {

        if (!doorType.equalsIgnoreCase("in") && !doorType.equalsIgnoreCase("out")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Door type must be 'in' or 'out'"));
        }

        try {
            parkingSlotService.controlDoor(hubId, lotId, doorType, action).get();
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "hubId", hubId,
                    "lotId", lotId,
                    "door", doorType,
                    "actionApplied", action));
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to communicate with Firebase: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to check slot occupancy state.
     * Example: GET /api/v1/parking/hubs/1/lots/101/slots/A01
     */
    @GetMapping("/hubs/{hubId}/lots/{lotId}/slots/{slotCode}")
    public ResponseEntity<?> getSlotState(
            @PathVariable int hubId,
            @PathVariable int lotId,
            @PathVariable String slotCode) {

        try {
            Boolean occupied = parkingSlotService.getSlotOccupancy(hubId, lotId, slotCode).get();
            return ResponseEntity.ok(Map.of(
                    "hubId", hubId,
                    "lotId", lotId,
                    "slotCode", slotCode,
                    "occupied", occupied));
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve slot state: " + e.getMessage()));
        }
    }
}