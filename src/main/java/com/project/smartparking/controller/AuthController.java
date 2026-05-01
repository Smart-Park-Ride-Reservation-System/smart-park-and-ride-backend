package com.project.smartparking.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.project.smartparking.dto.LoginRequest;
import com.project.smartparking.dto.SignupRequest;
import com.project.smartparking.model.User;
import com.project.smartparking.service.AuthService;
import com.project.smartparking.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired                          
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        boolean isValid = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        if (isValid) {
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

    @PostMapping("/signup")             
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {  

        if (!request.getPassword().equals(request.getVerifyPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Passwords do not match");   
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {  // ✅ Instance call
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already exists");     
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());

        User savedUser = userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

}