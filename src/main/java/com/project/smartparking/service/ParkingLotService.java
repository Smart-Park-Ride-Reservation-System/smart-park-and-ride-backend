package com.project.smartparking.service;

import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.repository.ParkingLotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingLotService {

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    public List<ParkingLot> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    public List<ParkingLot> getParkingLotsByHub(Long hubId) {
        return parkingLotRepository.findByHub_HubId(hubId);
    }

    public ParkingLot createParkingLot(ParkingLot parkingLot) {
        return parkingLotRepository.save(parkingLot);
    }

    public ParkingLot updateParkingLot(Long id, ParkingLot updated) {
        ParkingLot existing = parkingLotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lot not found: " + id));

        existing.setLotCode(updated.getLotCode());
        existing.setName(updated.getName());
        existing.setLotType(updated.getLotType());
        existing.setStatus(updated.getStatus());
        existing.setTotalSlots(updated.getTotalSlots());

        if (updated.getHub() != null) {
            Hub hub = new Hub();
            hub.setHubId(updated.getHub().getHubId());
            existing.setHub(hub);
        }

        return parkingLotRepository.save(existing);
    }

    public void deleteParkingLotById(Long id) {
        parkingLotRepository.deleteById(id);
    }
}