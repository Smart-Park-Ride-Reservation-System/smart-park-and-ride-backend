package com.project.smartparking.controller;

import com.project.smartparking.dto.GlobalSettingsDTO;
import com.project.smartparking.service.GlobalSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GlobalSettingsController {

    private final GlobalSettingsService globalSettingsService;

    @GetMapping
    public ResponseEntity<GlobalSettingsDTO> getSettings() {
        return ResponseEntity.ok(globalSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<GlobalSettingsDTO> updateSettings(@RequestBody GlobalSettingsDTO dto) {
        return ResponseEntity.ok(globalSettingsService.updateSettings(dto));
    }
}