package com.project.smartparking.controller;

import com.project.smartparking.model.User;
import com.project.smartparking.repository.AuthRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AuthRepository repo;

    // GET
    @GetMapping
    public List<User> getAllUsers() {
        return repo.findAll();
    }

    // DELETE USER
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        repo.deleteById(id);
    }

    // UPDATE STATUS
    @PatchMapping("/{id}")
    public User updateStatus(
            @PathVariable Long id,
            @RequestBody User request
    ) {

        User user = repo.findById(id).orElseThrow();

        user.setStatus(request.getStatus());

        return repo.save(user);
    }

    // UPDATE ROLE
    @PatchMapping("/{id}/role")
    public User updateRole(
            @PathVariable Long id,
            @RequestBody User request
    ) {

        User user = repo.findById(id).orElseThrow();

        user.setRole(request.getRole());

        return repo.save(user);
    }
}