package com.project.smartparking.controller;

import com.project.smartparking.model.Hub;
import com.project.smartparking.repository.HubRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hubs")
@CrossOrigin(origins = "http://localhost:3000")
public class HubController {

    @Autowired
    private HubRepository repo;

    // GET ALL HUBS
    @GetMapping
    public List<Hub> getAll() {
        return repo.findAll();
    }

    // CREATE HUB
    @PostMapping
    public Hub create(@RequestBody Hub hub) {
        return repo.save(hub);
    }

    // UPDATE HUB
    @PutMapping("/{id}")
    public Hub update(@PathVariable Long id, @RequestBody Hub hub) {

        Hub existing = repo.findById(id).orElseThrow();

        existing.setName(hub.getName());
        existing.setProvince(hub.getProvince());
        existing.setDistrict(hub.getDistrict());
        existing.setCity(hub.getCity());
        existing.setGpsLocation(hub.getGpsLocation());
        existing.setStatus(hub.getStatus());
        return repo.save(existing);
    }

    // DELETE HUB
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}