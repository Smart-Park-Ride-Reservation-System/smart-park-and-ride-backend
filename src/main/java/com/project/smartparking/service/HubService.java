package com.project.smartparking.service;

import com.project.smartparking.dto.HubSummaryDto;
import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.repository.ParkingLotRepository;
import com.project.smartparking.repository.ParkingSlotRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HubService {

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;


    // GET ALL
    public List<Hub> getAllHubs() {
        return hubRepository.findAll();
    }

    // CREATE
    @Transactional
    public Hub createHub(Hub hub) {
        if (hub.getName() == null || hub.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Hub name is required");
        }
        return hubRepository.save(hub);
    }

    // UPDATE
    @Transactional
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
    @Transactional
    public void deleteHub(Long id) {
        if (!hubRepository.existsById(id)) {
            throw new RuntimeException("Hub not found: " + id);
        }
        hubRepository.deleteById(id);
    }


    public List<HubSummaryDto> getActiveHubSummaries() {
    List<Hub> activeHubs = hubRepository.findByStatusIgnoreCase("ACTIVE");
    if (activeHubs.isEmpty()) {
        return List.of();
    }

    return activeHubs.stream()
            .map(this::toSummary)
            .toList();
}

    private HubSummaryDto toSummary(Hub hub) {
        List<ParkingLot> activeLots = parkingLotRepository.findByHub_HubId(hub.getHubId()).stream()
                .filter(lot -> lot.getStatus() == ParkingLot.LotStatus.ACTIVE)
                .toList();

        int totalSlots = activeLots.stream()
                .mapToInt(lot -> lot.getTotalSlots() != null ? lot.getTotalSlots() : 0)
                .sum();

        Set<Long> activeLotIds = activeLots.stream()
                .map(ParkingLot::getLotId)
                .collect(Collectors.toSet());

      int availableSlots = parkingSlotRepository.countAvailableByHub(hub.getHubId());

        return HubSummaryDto.builder()
                .hubId(hub.getHubId())
                .name(hub.getName())
                .city(hub.getCity())
                .province(hub.getProvince())
                .mapUrl(hub.getGpsLocation())
                .status(hub.getStatus())
                .availableSlots(availableSlots)
                .totalSlots(totalSlots)
                .build();
    }
}