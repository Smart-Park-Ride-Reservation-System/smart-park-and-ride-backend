package com.project.smartparking.service;

import com.project.smartparking.model.Hub;
import com.project.smartparking.repository.HubRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HubService {

    @Autowired
    private HubRepository hubRepository;

    // GET ALL
    public List<Hub> getAllHubs() {
        return hubRepository.findAll();
    }

    // CREATE
    public Hub createHub(Hub hub) {
        if (hub.getName() == null || hub.getName().trim().isEmpty()) {
            throw new RuntimeException("Hub name is required");
        }
        return hubRepository.save(hub);
    }

    // UPDATE
    public Hub updateHub(Long id, Hub hub) {
        Hub existing = hubRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Hub not found: " + id));

        existing.setName(hub.getName());
        existing.setProvince(hub.getProvince());
        existing.setDistrict(hub.getDistrict());
        existing.setCity(hub.getCity());
        existing.setGpsLocation(hub.getGpsLocation());
        existing.setStatus(hub.getStatus());

        return hubRepository.save(existing);
    }

    // DELETE
    public void deleteHub(Long id) {
        if (!hubRepository.existsById(id)) {
            throw new RuntimeException("Hub not found: " + id);
        }
        hubRepository.deleteById(id);
    }
}