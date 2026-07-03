package com.project.smartparking.service;

import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.ParkingSlot.SlotStatus;

import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParkingSlotService {

    @Autowired
    private ParkingSlotRepository slotRepo;

    @Autowired
    private HubRepository hubRepo;

    @Autowired
    private ReservationRepository reservationRepo;


    public List<ParkingSlot> getAll() {
        return slotRepo.findAll();
    }

    public List<ParkingSlot> findAvailableByHub(Long hubId) {
        return slotRepo.findByHub(hubId);
    }

    public ParkingSlot updateStatus(Long id, ParkingSlot.SlotStatus status) {
        ParkingSlot slot = slotRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));
        slot.setStatus(status);
        return slotRepo.save(slot);
    }

    public List<Hub> getAllHubs() {
        return hubRepo.findAll();
    }

    public void generateSlots(Long lotId, List<List<String>> grid) {
        slotRepo.deleteByLotId(lotId);

        List<ParkingSlot> slots = new ArrayList<>();
        for (List<String> row : grid) {
            for (String slotCode : row) {
                ParkingSlot slot = new ParkingSlot();
                slot.setSlotCode(slotCode);
                slot.setLotId(lotId);
                slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
                slots.add(slot);
            }
        }
        slotRepo.saveAll(slots);
    }

    

    // ParkingSlotService.java
    public List<ParkingSlot> findByLot(Long hubId, Long lotId) { // ✅ add hubId param
        return slotRepo.findByLotId(lotId);
    }

    public void deleteSlot(Long id) {
        if (!slotRepo.existsById(id)) {
            throw new RuntimeException("Slot not found: " + id);
        }
        slotRepo.deleteById(id);
    }
}