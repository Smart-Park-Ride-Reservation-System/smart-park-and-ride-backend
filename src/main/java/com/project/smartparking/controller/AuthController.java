package com.project.smartparking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.project.smartparking.dto.ForgotPasswordRequest;
import com.project.smartparking.dto.LoginRequest;
import com.project.smartparking.dto.ResetPasswordRequest;
import com.project.smartparking.dto.SignupRequest;
import com.project.smartparking.model.User;
import com.project.smartparking.service.AuthService;
import com.project.smartparking.service.JwtService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
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

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().name());
            response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            response.put("userId", user.getUserId());
            response.put("hub", toHubPayload(user));

            return ResponseEntity.ok(response);
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Email is required");
        }

        authService.initiatePasswordReset(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message", "If an account with that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {

        if (request.getToken() == null || request.getToken().isBlank()
                || request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Token and new password are required");
        }

        boolean success = authService.resetPassword(request.getToken(), request.getNewPassword());

        if (!success) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Reset link is invalid or has expired");
        }

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid token structure");
        }

        String token = bearerToken.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            User user = authService.findUserByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("email", user.getEmail());
            response.put("role", user.getRole().name());
            response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            response.put("hub", toHubPayload(user));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
    }

    private Map<String, Object> toHubPayload(User user) {
        if (user.getHub() == null)
            return null;
        Map<String, Object> hub = new HashMap<>();
        hub.put("hubId", user.getHub().getHubId());
        hub.put("name", user.getHub().getName());
        return hub;
    }
}