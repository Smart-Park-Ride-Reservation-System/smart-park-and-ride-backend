package com.project.smartparking.controller;

import com.project.smartparking.model.User;
import com.project.smartparking.model.Hub;
import com.project.smartparking.repository.AuthRepository;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.dto.UserCreateUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
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

    // GET
    @GetMapping
    public List<User> getAllUsers() {
        return repo.findAll();
    }

    // CREATE USER
    @PostMapping
    public User createUser(@RequestBody UserCreateUpdateRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "default123"));
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        if (request.getHubId() != null && (request.getRole() == User.Role.ATTENDANT || request.getRole() == User.Role.HUB_ADMIN)) {
            Hub hub = hubRepo.findById(request.getHubId()).orElse(null);
            user.setHub(hub);
        }

        return repo.save(user);
    }

    // DELETE USER
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        repo.deleteById(id);
    }

    // UPDATE USER
    @PatchMapping("/{id}")
    public User updateUser(
            @PathVariable Long id,
            @RequestBody UserCreateUpdateRequest request
    ) {

        User user = repo.findById(id).orElseThrow();

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        if (request.getHubId() != null && (user.getRole() == User.Role.ATTENDANT || user.getRole() == User.Role.HUB_ADMIN)) {
            Hub hub = hubRepo.findById(request.getHubId()).orElse(null);
            user.setHub(hub);
        } else if (user.getRole() != User.Role.ATTENDANT && user.getRole() != User.Role.HUB_ADMIN) {
            user.setHub(null);
        }

        return repo.save(user);
    }

    // UPDATE ROLE
    @PatchMapping("/{id}/role")
    public User updateRole(
            @PathVariable Long id,
            @RequestBody UserCreateUpdateRequest request
    ) {

        User user = repo.findById(id).orElseThrow();
        user.setRole(request.getRole());
        if (user.getRole() != User.Role.ATTENDANT && user.getRole() != User.Role.HUB_ADMIN) {
            user.setHub(null);
        }
        return repo.save(user);
    }
}