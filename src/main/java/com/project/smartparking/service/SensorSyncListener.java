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
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.COMPLETED);

    private final ParkingSlotRepository slotRepo;
    private final ReservationRepository reservationRepo;

    @Autowired
    private SlotStreamService slotStreamService;

    @Autowired
    private SensorActivityService activityService;

    public SensorSyncListener(ParkingSlotRepository slotRepo,
            ReservationRepository reservationRepo) {
        this.slotRepo = slotRepo;
        this.reservationRepo = reservationRepo;
    }

    @PostConstruct
    public void init() {
        DatabaseReference hubsRef = FirebaseDatabase.getInstance().getReference("sensor/hubs");

        hubsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot hubSnap : snapshot.getChildren()) {
                    String hubKey = hubSnap.getKey();
                    if (hubKey == null || !hubKey.startsWith("hub_")) continue;
                    Long hubId;
                    try {
                        hubId = Long.parseLong(hubKey.substring(4));
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    DataSnapshot lotsSnap = hubSnap.child("lots");
                    if (lotsSnap.exists()) {
                        for (DataSnapshot lotSnap : lotsSnap.getChildren()) {
                            String lotKey = lotSnap.getKey(); // e.g. "lot_1"
                            if (lotKey == null || !lotKey.startsWith("lot_")) continue;

                            Long lotId;
                            try {
                                lotId = Long.parseLong(lotKey.substring(4));
                            } catch (NumberFormatException e) {
                                continue;
                            }

                            for (DataSnapshot slotSnap : lotSnap.getChildren()) {
                                String slotCode = slotSnap.getKey();
                                Boolean occupied = slotSnap.child("occupied").getValue(Boolean.class);

                                if (occupied != null && slotCode != null) {
                                    updateSlotStatusInDb(hubId, lotId, slotCode, occupied);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Firebase sensor listener cancelled: {}", error.getMessage());
            }
        });

        log.info("[SensorSync] listening on /sensor/hubs for dynamic sensor updates");
    }

   
    public void updateSlotStatusInDb(Long hubId, Long lotId, String slotCode, boolean occupied) {
        slotRepo.findByLot_LotIdAndSlotCode(lotId, slotCode).ifPresentOrElse(slot -> {
            if (slot.getStatus() == SlotStatus.OUT_OF_SERVICE) {
                activityService.log("WARN",
                        String.format("hub_%d/lot_%d/%s ping ignored — slot OUT_OF_SERVICE", hubId, lotId, slotCode));
                return;
            }

            activityService.log("INFO",
                    String.format("hub_%d/lot_%d/%s occupied=%s", hubId, lotId, slotCode, occupied));

            SlotStatus newStatus;

            if (occupied) {
                newStatus = SlotStatus.OCCUPIED;
            } else {
                List<Reservation> activeReservations = reservationRepo
                        .findBySlotSlotIdAndStatusIn(slot.getSlotId(), ACTIVE_RESERVATION_STATUSES);

                boolean hasActiveReservation = !activeReservations.isEmpty();
                newStatus = hasActiveReservation ? SlotStatus.RESERVED : SlotStatus.AVAILABLE;
            }

            if (slot.getStatus() != newStatus) {
                slot.setStatus(newStatus);
                slotRepo.save(slot);
                log.info("Live Update: Slot {} (lot={}, hub={}) is now {}", slotCode, lotId, hubId, newStatus);
                activityService.log("INFO",
                        String.format("Slot %s status changed -> %s", slotCode, newStatus));
            }

            
            slotStreamService.notifyLotChange(hubId, lotId);
            pushToFirebase(slot);

        }, () -> {
            log.warn("[SensorSync] No DB slot for hubId={} lotId={} slotCode={}", hubId, lotId, slotCode);
            activityService.log("ERROR",
                    String.format("No DB slot found for hub_%d/lot_%d/%s", hubId, lotId, slotCode));
        });
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

