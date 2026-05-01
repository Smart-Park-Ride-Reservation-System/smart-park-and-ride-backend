package com.project.smartparking.service;

import com.project.smartparking.model.Reservation;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.repository.ReservationRepository;
import com.project.smartparking.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReservationService {

    @Autowired private ReservationRepository reservationRepo;
    @Autowired private ParkingSlotRepository slotRepo;

    public List<Reservation> getAll() {
        return reservationRepo.findAll();
    }

    public Reservation getById(Long id) {
        return reservationRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
    }

    public List<Reservation> getByUser(Long userId) {
        return reservationRepo.findByUserId(userId);
    }

    public Reservation create(Reservation reservation) {

        ParkingSlot slot = slotRepo.findById(reservation.getSlotId())
            .orElseThrow(() -> new RuntimeException("Slot not found"));

        slot.setStatus(ParkingSlot.SlotStatus.RESERVED);
        slotRepo.save(slot);

        return reservationRepo.save(reservation);
    }

    public Reservation updateStatus(Long id, String status) {

        Reservation res = getById(id);

        res.setStatus(status);

        ParkingSlot slot = slotRepo.findById(res.getSlotId())
            .orElseThrow(() -> new RuntimeException("Slot not found"));

        if ("ACTIVE".equals(status)) {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        } 
        else if ("COMPLETED".equals(status)) {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        } 
        else if ("CANCELLED".equals(status)) {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        }

        slotRepo.save(slot);

        return reservationRepo.save(res);
    }

    public void delete(Long id) {

        Reservation res = getById(id);

        ParkingSlot slot = slotRepo.findById(res.getSlotId())
            .orElseThrow(() -> new RuntimeException("Slot not found"));

        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        slotRepo.save(slot);

        reservationRepo.deleteById(id);
    }
}