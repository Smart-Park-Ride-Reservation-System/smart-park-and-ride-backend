package com.project.smartparking.service;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.repository.ParkingSlotRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Keeps ParkingSlot.status in sync with live sensor readings at
 * sensor/parking/{sensorId}/occupied in Firebase Realtime Database.
 *
 * IMPORTANT: the Firebase node key is the physical sensor's ID
 * (e.g. "A01"), NOT necessarily the slot_code. Multiple lots can share
 * the same slot_code (e.g. every lot has an "A02"), but only ONE
 * parking_slots row may have a given sensor_id — that uniqueness is
 * enforced by a DB constraint (uq_parking_slots_sensor_id). Joining on
 * sensor_id instead of slot_code is what makes this disambiguation work.
 *
 * This class only READS from Firebase and writes to the relational DB.
 * It must never write occupancy back to Firebase, or you get an
 * infinite update loop with the sensors.
 */
@Service
public class FirebaseSlotSyncService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseSlotSyncService.class);

    @Autowired
    private ParkingSlotRepository slotRepo;

    private DatabaseReference parkingRef() {
        return FirebaseDatabase.getInstance().getReference("sensor").child("parking");
    }

    @PostConstruct
    public void attachListener() {
        parkingRef().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                syncSlot(snapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                syncSlot(snapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                log.debug("Firebase node removed for sensor '{}', DB row left untouched", snapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // Not applicable to this data shape.
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Firebase sensor/parking listener cancelled: {}", error.getMessage());
            }
        });

        log.info("FirebaseSlotSyncService: attached real-time listener on sensor/parking");
    }

    private void syncSlot(DataSnapshot snapshot) {
        String sensorId = snapshot.getKey();
        Boolean occupied = snapshot.child("occupied").getValue(Boolean.class);

        if (sensorId == null || occupied == null) {
            log.warn("Ignoring malformed sensor payload at sensor/parking/{}", sensorId);
            return;
        }

        Optional<ParkingSlot> maybeSlot = slotRepo.findBySensor_SensorId(sensorId);
        if (maybeSlot.isEmpty()) {
            log.warn("Sensor update for unregistered sensor_id '{}' - no matching DB slot, ignoring. " +
                    "Assign this sensor_id to a parking_slots row to enable sync.", sensorId);
            return;
        }

        ParkingSlot slot = maybeSlot.get();

        // Don't let a sensor reading override an admin-set state like
        // MAINTENANCE or RESERVED. Only toggle between AVAILABLE/OCCUPIED.
        if (!isSensorControlled(slot.getStatus())) {
            log.debug("Slot (sensor_id={}) is in manual state {}, skipping sensor sync", sensorId, slot.getStatus());
            return;
        }

        ParkingSlot.SlotStatus newStatus = occupied
                ? ParkingSlot.SlotStatus.OCCUPIED
                : ParkingSlot.SlotStatus.AVAILABLE;

        if (slot.getStatus() != newStatus) {
            slot.setStatus(newStatus);
            // Save directly via the repo — NOT slotService.updateStatus() —
            // so this never triggers a write back to Firebase.
            slotRepo.save(slot);
            log.info("Synced slot_code={} (sensor_id={}) -> {} (from Firebase sensor)",
                    slot.getSlotCode(), sensorId, newStatus);
        }
    }

    private boolean isSensorControlled(ParkingSlot.SlotStatus status) {
        return status == ParkingSlot.SlotStatus.AVAILABLE
                || status == ParkingSlot.SlotStatus.OCCUPIED;
    }
}