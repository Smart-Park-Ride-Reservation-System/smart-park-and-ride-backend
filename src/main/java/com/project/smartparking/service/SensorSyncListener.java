package com.project.smartparking.service;

import com.google.firebase.database.*;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.ParkingSlot.SlotStatus;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.ReservationRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorSyncListener {

    private static final Logger log = LoggerFactory.getLogger(SensorSyncListener.class);
    private static final List<String> ACTIVE_RESERVATION_STATUSES = List.of("CONFIRMED", "CHECKED_IN");

    private final ParkingSlotRepository slotRepo;
    private final ReservationRepository reservationRepo;

    public SensorSyncListener(ParkingSlotRepository slotRepo,
                               ReservationRepository reservationRepo) {
        this.slotRepo = slotRepo;
        this.reservationRepo = reservationRepo;
    }

    @PostConstruct
    public void init() {
        DatabaseReference parkingSensorsRef = FirebaseDatabase.getInstance().getReference("sensor/parking");

        parkingSensorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot zoneSnap : snapshot.getChildren()) {
                    String zone = zoneSnap.getKey(); // "A" or "B"
                    for (DataSnapshot slotSnap : zoneSnap.getChildren()) {
                        String slotNo = slotSnap.getKey(); // "05"
                        Boolean occupied = slotSnap.child("occupied").getValue(Boolean.class);

                        if (occupied != null) {
                            String fullSlotNumber = zone + slotNo; // "A05" or "B05"
                            updateSlotStatusInDb(fullSlotNumber, occupied);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Firebase sensor listener cancelled: {}", error.getMessage());
            }
        });
    }

    private void updateSlotStatusInDb(String slotNumber, boolean occupied) {
        slotRepo.findBySlotNumber(slotNumber).ifPresent(slot -> {
            if (slot.getStatus() == SlotStatus.OUT_OF_SERVICE) {
                return;
            }

            // Don't mark as AVAILABLE if a reservation is active
            boolean hasActiveReservation = !reservationRepo
                    .findBySlotIdAndStatusIn(slot.getSlotId(), ACTIVE_RESERVATION_STATUSES)
                    .isEmpty();

            if (!occupied && hasActiveReservation) {
                return;
            }

            SlotStatus newStatus = occupied ? SlotStatus.OCCUPIED : SlotStatus.AVAILABLE;
            if (slot.getStatus() != newStatus) {
                slot.setStatus(newStatus);
                slotRepo.save(slot);
                log.info("Live Update: Slot {} is now {}", slotNumber, newStatus);
            }
        });
    }
}