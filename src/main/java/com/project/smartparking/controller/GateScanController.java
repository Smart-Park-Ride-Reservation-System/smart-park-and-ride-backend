package com.project.smartparking.controller;

import com.project.smartparking.service.GateScanService;
import com.project.smartparking.service.GateScanService.GateType;
import com.project.smartparking.service.GateScanService.ScanResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations/gate")
public class GateScanController {

    private final GateScanService gateScanService;

    public GateScanController(GateScanService gateScanService) {
        this.gateScanService = gateScanService;
    }

    @PostMapping("/scan")
    public ResponseEntity<ScanResult> scan(@RequestBody GateScanRequest request) {
        // Validation check for missing fields
        if (request == null || request.qrToken() == null || request.gateType() == null) {
            return ResponseEntity.badRequest()
                    .body(ScanResult.rejected("Missing required request fields: qrToken and gateType are required."));
        }

        // Safely parse GateType case-insensitively
        GateType gateType;
        try {
            gateType = GateType.valueOf(request.gateType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ScanResult.rejected("Invalid gate type: " + request.gateType() + ". Must be ENTRY or EXIT."));
        }

        ScanResult result = gateScanService.handleScan(
                request.qrToken().trim(),
                request.gateId() != null ? request.gateId().trim() : "GATE-01",
                gateType
        );

        return ResponseEntity.ok(result);
    }

    public record GateScanRequest(String qrToken, String gateId, String gateType) {}
}