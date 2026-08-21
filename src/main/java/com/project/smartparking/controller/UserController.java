package com.project.smartparking.controller;

import com.project.smartparking.model.User;
import com.project.smartparking.model.Hub;
import com.project.smartparking.repository.AuthRepository;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.dto.UserCreateUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AuthRepository repo;

    @Autowired
    private HubRepository hubRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // GET CURRENT USER PROFILE — any authenticated user, viewing their own record
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return ResponseEntity.ok(user);
    }

    // UPDATE CURRENT USER PROFILE — any authenticated user, editing their own record
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    public ResponseEntity<User> updateCurrentUser(@RequestBody UserCreateUpdateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getVehicles() != null) {
            user.setVehicles(request.getVehicles());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        User updatedUser = repo.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    // GET ALL USERS — matches config: GET /api/users/** -> ADMIN, HUB_ADMIN
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @GetMapping
    public List<User> getAllUsers() {
        return repo.findAll();
    }

    // CREATE USER — matches config: non-GET /api/users/** -> ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public User createUser(@RequestBody UserCreateUpdateRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setVehicles(request.getVehicles());
        user.setPassword(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "default123"));
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        if (request.getHubId() != null
                && (request.getRole() == User.Role.ATTENDANT || request.getRole() == User.Role.HUB_ADMIN)) {
            Hub hub = hubRepo.findById(request.getHubId()).orElse(null);
            user.setHub(hub);
        }

        return repo.save(user);
    }

    // DELETE USER — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        repo.deleteById(id);
    }

    // UPDATE USER BY ID (ADMIN USE) — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public User updateUser(
            @PathVariable Long id,
            @RequestBody UserCreateUpdateRequest request) {
        User user = repo.findById(id).orElseThrow();

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getRole() != null)
            user.setRole(request.getRole());
        if (request.getStatus() != null)
            user.setStatus(request.getStatus());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getVehicles() != null)
            user.setVehicles(request.getVehicles());

        if (request.getHubId() != null
                && (user.getRole() == User.Role.ATTENDANT || user.getRole() == User.Role.HUB_ADMIN)) {
            Hub hub = hubRepo.findById(request.getHubId()).orElse(null);
            user.setHub(hub);
        } else if (user.getRole() != User.Role.ATTENDANT && user.getRole() != User.Role.HUB_ADMIN) {
            user.setHub(null);
        }

        return repo.save(user);
    }

    // UPDATE ROLE — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public User updateRole(
            @PathVariable Long id,
            @RequestBody UserCreateUpdateRequest request) {
        User user = repo.findById(id).orElseThrow();
        user.setRole(request.getRole());
        if (user.getRole() != User.Role.ATTENDANT && user.getRole() != User.Role.HUB_ADMIN) {
            user.setHub(null);
        }
        return repo.save(user);
    }
}