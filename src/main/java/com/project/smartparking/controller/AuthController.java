package com.project.smartparking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.smartparking.dto.LoginRequest;
import com.project.smartparking.dto.SignupRequest;
import com.project.smartparking.model.User;
import com.project.smartparking.service.AuthService;
import com.project.smartparking.service.JwtService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        User user = authService.loginUser(request.getEmail(), request.getPassword());

        if (user != null) {
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole().name(),
                    "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                    "userId", user.getUserId()
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid email or password");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {

        User savedUser = authService.signup(request);

        if (savedUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Signup failed");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Account created successfully",
                        "userId", savedUser.getUserId()));
    }
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String bearerToken
    ) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid token structure");
        }

                    String token = bearerToken.substring(7);

        try {
            
            String email = jwtService.extractEmail(token); 

            User user = authService.findUserByEmail(email); 

            if (user != null) {
                return ResponseEntity.ok(Map.of(
                        "userId", user.getUserId(),
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "firstName", user.getFirstName() != null ? user.getFirstName() : ""
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
    }
}