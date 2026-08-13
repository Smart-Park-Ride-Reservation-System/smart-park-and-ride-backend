package com.project.smartparking.controller;

import com.project.smartparking.service.GateScanService;
import com.project.smartparking.service.GateScanService.GateType;
import com.project.smartparking.service.GateScanService.ScanResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations/gate")
public class GateScanController {

    private final GateScanService gateScanService;

    public GateScanController(GateScanService gateScanService) {
        this.gateScanService = gateScanService;
    }

    @PostMapping("/scan")
    public ScanResult scan(@RequestBody GateScanRequest request) {
        return gateScanService.handleScan(
                request.qrToken(),
                request.gateId(),
                GateType.valueOf(request.gateType())
        );
    }

    public record GateScanRequest(String qrToken, String gateId, String gateType) {}
}