package com.project.smartparking.controller;

import com.project.smartparking.model.Hub;
import com.project.smartparking.repository.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hubs")
public class HubController {

    @Autowired
    private HubRepository repo;

    // Open read access for all roles including commuters
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN', 'ATTENDANT', 'COMMUTER')")
    @GetMapping
    public List<Hub> getAll() {
        return repo.findAll();
    }

    // CREATE HUB: Admin only
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @PostMapping
    public Hub create(@RequestBody Hub hub) {
        return repo.save(hub);
    }

    // UPDATE HUB: Admin only
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
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

    // DELETE HUB: Admin only
    @PreAuthorize("hasAnyRole('ADMIN', 'HUB_ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}