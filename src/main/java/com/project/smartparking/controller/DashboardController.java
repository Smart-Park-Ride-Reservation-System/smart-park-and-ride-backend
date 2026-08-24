package com.project.smartparking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminDashboard() {
        return ResponseEntity.ok("ADMIN DASHBOARD");
    }

    @GetMapping("/hub-admin")
    @PreAuthorize("hasRole('HUB_ADMIN')")
    public ResponseEntity<String> hubAdminDashboard() {
        return ResponseEntity.ok("HUB ADMIN DASHBOARD");
    }

    @GetMapping("/attendant")
    @PreAuthorize("hasRole('ATTENDANT')")
    public ResponseEntity<String> attendantDashboard() {
        return ResponseEntity.ok("ATTENDANT DASHBOARD");
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> userDashboard() {
        return ResponseEntity.ok("USER DASHBOARD");
    }
}