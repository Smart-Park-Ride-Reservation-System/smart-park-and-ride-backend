package com.project.smartparking.service;

import com.google.firebase.database.*;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.ParkingSlot.SlotStatus;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.model.Reservation.ReservationStatus;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.ReservationRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DependsOn("firebaseConfig")
public class SensorSyncListener {

    private static final Logger log = LoggerFactory.getLogger(SensorSyncListener.class);

    // Updated to use ReservationStatus enums instead of String literals
    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN);

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
                    String zone = zoneSnap.getKey(); // e.g., "A", "B"
                    for (DataSnapshot slotSnap : zoneSnap.getChildren()) {
                        String slotNo = slotSnap.getKey(); // raw sensor key
                        Boolean occupied = slotSnap.child("occupied").getValue(Boolean.class);

                        if (occupied != null && zone != null && slotNo != null) {
                            String slotCode = normalizeSlotCode(zone, slotNo);
                            updateSlotStatusInDb(slotCode, occupied);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Firebase sensor listener cancelled: {}", error.getMessage());
            }
        });

        log.info("[SensorSync] listening on /sensor/parking for dynamic sensor updates");
    }

    private String normalizeSlotCode(String zone, String slotNo) {
        try {
            int n = Integer.parseInt(slotNo);
            return zone + String.format("%02d", n);
        } catch (NumberFormatException e) {
            return zone + slotNo;
        }
    }

    private void updateSlotStatusInDb(String slotCode, boolean occupied) {
        slotRepo.findBySlotCode(slotCode).ifPresentOrElse(slot -> {
            if (slot.getStatus() == SlotStatus.OUT_OF_SERVICE) {
                return;
            }

            SlotStatus newStatus;

            if (occupied) {
                newStatus = SlotStatus.OCCUPIED;
            } else {
                // Compatible with the updated ReservationRepository signature:
                // Collection<ReservationStatus>
                List<Reservation> activeReservations = reservationRepo
                        .findBySlotSlotIdAndStatusIn(slot.getSlotId(), ACTIVE_RESERVATION_STATUSES);

                boolean hasActiveReservation = !activeReservations.isEmpty();
                newStatus = hasActiveReservation ? SlotStatus.RESERVED : SlotStatus.AVAILABLE;
            }

            if (slot.getStatus() != newStatus) {
                slot.setStatus(newStatus);
                slotRepo.save(slot);
                pushToFirebase(slot);
                log.info("Live Update: Slot {} is now {}", slotCode, newStatus);
            }
        }, () -> log.debug("Unregistered slot sensor payload received for slotCode={}", slotCode));
    }

    private void pushToFirebase(ParkingSlot slot) {
        DatabaseReference slotRef = FirebaseDatabase.getInstance()
                .getReference("slots/" + slot.getSlotId());

        Map<String, Object> update = new HashMap<>();
        update.put("status", slot.getStatus().name());
        update.put("manualOverride", false);
        update.put("source", "sensor");
        update.put("updatedAt", Instant.now().toString());

        slotRef.updateChildrenAsync(update);
    }
}