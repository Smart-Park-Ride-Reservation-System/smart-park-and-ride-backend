package com.project.smartparking.service;

import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingSlotService {

    @Autowired private ParkingSlotRepository slotRepo;
    @Autowired private HubRepository hubRepo;

    public List<ParkingSlot> getAll() {
        return slotRepo.findAll();
    }

    public List<ParkingSlot> findAvailableByHub(Long hubId) {
        return slotRepo.findAvailableByHub(hubId);
    }

    public ParkingSlot updateStatus(Long id, ParkingSlot.SlotStatus status) {
        ParkingSlot slot = slotRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Slot not found"));

        slot.setStatus(status);
        return slotRepo.save(slot);
    }

    public List<Hub> getAllHubs() {
        return hubRepo.findAll();
    }
}