package com.project.smartparking.controller;

import com.project.smartparking.model.User;
import com.project.smartparking.model.User.Role;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/home")
    public ResponseEntity<?> dashboard(HttpSession session) {

        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Please login first");
        }

        Role role = user.getRole();

        if (role == Role.ADMIN) {
            return ResponseEntity.ok("ADMIN DASHBOARD");
        }

        if (role == Role.ATTENDANT) {
            return ResponseEntity.ok("ATTENDANT DASHBOARD");
        }

        return ResponseEntity.ok("USER DASHBOARD");
    }
}