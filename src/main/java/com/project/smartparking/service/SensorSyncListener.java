package com.project.smartparking.service;

import com.google.firebase.database.*;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.ParkingSlot.SlotStatus;
import com.project.smartparking.model.Sensor;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.ReservationRepository;
import com.project.smartparking.repository.SensorRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SensorSyncListener {

    private static final Logger log = LoggerFactory.getLogger(SensorSyncListener.class);

    private static final List<String> ACTIVE_RESERVATION_STATUSES = List.of("CONFIRMED", "CHECKED_IN");

    private final SensorRepository sensorRepo;
    private final ParkingSlotRepository slotRepo;
    private final ReservationRepository reservationRepo;

    public SensorSyncListener(SensorRepository sensorRepo,
            ParkingSlotRepository slotRepo,
            ReservationRepository reservationRepo) {
        this.sensorRepo = sensorRepo;
        this.slotRepo = slotRepo;
        this.reservationRepo = reservationRepo;
    }

    @PostConstruct
    public void init() {
        DatabaseReference sensorsRef = FirebaseDatabase.getInstance().getReference("sensors");

        sensorsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                sync(snapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                sync(snapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Firebase 'sensors' listener cancelled: {}", error.getMessage());
            }
        });

        log.info("Attached Firebase listener on /sensors");
    }

    private void sync(DataSnapshot snapshot) {
        String macAddress = snapshot.getKey();
        Long slotId = snapshot.child("slotId").getValue(Long.class);
        Double rawValue = snapshot.child("value").getValue(Double.class);
        String type = snapshot.child("type").getValue(String.class);

        if (rawValue == null) {
            log.warn("Sensor {} sent no 'value' field, skipping", macAddress);
            return;
        }

        Sensor sensor = sensorRepo.findByMacAddress(macAddress)
                .orElseGet(() -> Sensor.builder()
                        .macAddress(macAddress)
                        .slotId(slotId)
                        .sensorType(type)
                        .build());

        sensor.setLastValue(BigDecimal.valueOf(rawValue));
        sensor.setStatus("ONLINE");
        if (slotId != null)
            sensor.setSlotId(slotId);
        sensorRepo.save(sensor);

        if (slotId == null) {
            log.warn("Sensor {} has no linked slotId, cannot update slot status", macAddress);
            return;
        }

        applyToSlot(slotId, rawValue);
    }

    private void applyToSlot(Long slotId, double rawValue) {
        boolean occupied = rawValue > 0;

        slotRepo.findById(slotId).ifPresentOrElse(slot -> {
            SlotStatus current = slot.getStatus();

            if (current == SlotStatus.OUT_OF_SERVICE) {
                return;
            }

            boolean hasActiveReservation = !reservationRepo
                    .findBySlotIdAndStatusIn(slotId, ACTIVE_RESERVATION_STATUSES)
                    .isEmpty();

            if (hasActiveReservation) {
                return;
            }

            SlotStatus newStatus = occupied ? SlotStatus.OCCUPIED : SlotStatus.AVAILABLE;
            if (current != newStatus) {
                slot.setStatus(newStatus);
                slotRepo.save(slot);
            }
        }, () -> log.warn("No ParkingSlot found for id {} (referenced by sensor)", slotId));
    }
}