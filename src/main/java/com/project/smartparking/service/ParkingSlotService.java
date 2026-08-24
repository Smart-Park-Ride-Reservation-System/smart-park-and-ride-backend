package com.project.smartparking.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.repository.ParkingLotRepository;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional(readOnly = true)
public class ParkingSlotService {

    private static final Logger log = LoggerFactory.getLogger(ParkingSlotService.class);

    @Autowired
    private ParkingSlotRepository slotRepo;

    @Autowired
    private ParkingLotRepository lotRepo;

    @Autowired
    private HubRepository hubRepo;

    @Autowired
    private ReservationRepository reservationRepo;


    private DatabaseReference getSensorNodeRef(Long hubId, Long lotId) {
        return FirebaseDatabase.getInstance()
                .getReference("sensor")
                .child("hubs")
                .child("hub_" + hubId)
                .child("lots")
                .child("lot_" + lotId);
    }

    public List<ParkingSlot> getAllWithLotAndHub() {
        return slotRepo.findAllWithLotAndHub();
    }

    private DatabaseReference getSlotSensorRef(ParkingSlot slot) {
        Long hubId = slot.getLot().getHub().getHubId();
        Long lotId = slot.getLot().getLotId();
        return getSensorNodeRef(hubId, lotId).child(slot.getSlotCode()).child("occupied");
    }

    public CompletableFuture<Void> controlDoor(int hubId, int lotId, String doorType, String command) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String path = String.format("door/hubs/hub_%d/lots/lot_%d/%s",
                hubId, lotId, doorType.toLowerCase().trim());

        DatabaseReference doorRef = FirebaseDatabase.getInstance().getReference(path);

        log.info("Writing door command '{}' to path: {}", command, path);

        doorRef.setValue(command.toLowerCase().trim(), (error, ref) -> {
            if (error != null) {
                log.error("Failed to update door status at path {}: {}", path, error.getMessage());
                future.completeExceptionally(error.toException());
            } else {
                log.info("Door status updated successfully at path: {}", path);
                future.complete(null);
            }
        });

        return future;
    }

    public CompletableFuture<Boolean> getSlotOccupancy(int hubId, int lotId, String slotCode) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String path = String.format("sensor/hubs/hub_%d/lots/lot_%d/%s/occupied",
                hubId, lotId, slotCode.trim());

        DatabaseReference slotRef = FirebaseDatabase.getInstance().getReference(path);

        slotRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean occupied = snapshot.getValue(Boolean.class);
                    future.complete(occupied != null && occupied);
                } else {
                    log.warn("Path does not exist in Firebase RTDB: {}", path);
                    future.complete(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Database query cancelled for path {}: {}", path, error.getMessage());
                future.completeExceptionally(error.toException());
            }
        });

        return future;
    }

    public List<ParkingSlot> getAll() {
        return slotRepo.findAll();
    }

    public List<ParkingSlot> findAvailableByHub(Long hubId) {
        return slotRepo.findByLot_Hub_HubIdAndStatus(hubId, ParkingSlot.SlotStatus.AVAILABLE);
    }

    @Transactional
    public ParkingSlot updateStatus(Long id, ParkingSlot.SlotStatus status) {
        ParkingSlot slot = slotRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));
        slot.setStatus(status);
        return slotRepo.save(slot);
    }

    public List<Hub> getAllHubs() {
        return hubRepo.findAll();
    }

    @Transactional
    public void generateSlots(Long lotId, List<List<String>> grid) {
        ParkingLot lot = lotRepo.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found: " + lotId));

        slotRepo.deleteByLot_LotId(lotId);

        List<ParkingSlot> slots = new ArrayList<>();
        for (List<String> row : grid) {
            for (String slotCode : row) {
                ParkingSlot slot = new ParkingSlot();
                slot.setSlotCode(slotCode);
                slot.setLot(lot);
                slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
                slots.add(slot);
            }
        }
        List<ParkingSlot> savedSlots = slotRepo.saveAll(slots);

        for (ParkingSlot slot : savedSlots) {
            getSlotSensorRef(slot).runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    if (currentData.getValue() == null) {
                        currentData.setValue(false);
                    }
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                    if (error != null) {
                        log.error("Firebase transaction error for slot {}: {}", slot.getSlotCode(), error.getMessage());
                    }
                }
            });
        }
    }

    public List<ParkingSlot> findByLot(Long hubId, Long lotId) {
        java.time.ZoneId SRI_LANKA_ZONE = java.time.ZoneId.of("Asia/Colombo");
        LocalDateTime now = java.time.ZonedDateTime.now(SRI_LANKA_ZONE).toLocalDateTime();
        return findByLot(hubId, lotId, now, now.plusHours(2));
    }

    public List<ParkingSlot> findByLot(Long hubId, Long lotId, LocalDateTime startTime, LocalDateTime endTime) {
        List<ParkingSlot> slots = slotRepo.findByLot_LotId(lotId);

        java.time.ZoneId SRI_LANKA_ZONE = java.time.ZoneId.of("Asia/Colombo");
        LocalDateTime now = java.time.ZonedDateTime.now(SRI_LANKA_ZONE).toLocalDateTime();
        List<Long> reservedSlotIds = reservationRepo.findReservedSlotIdsInWindow(
                hubId, startTime, endTime, now.minusMinutes(2), now.minusMinutes(15));

        for (ParkingSlot slot : slots) {
            if (slot.getStatus() == ParkingSlot.SlotStatus.OUT_OF_SERVICE) {
                continue;
            }

            boolean isSensorOccupied = slot.getSensor() != null && slot.getSensor().isOccupiedSlot();

            if (isSensorOccupied || slot.getStatus() == ParkingSlot.SlotStatus.OCCUPIED) {
                slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            } else if (reservedSlotIds.contains(slot.getSlotId())) {
                slot.setStatus(ParkingSlot.SlotStatus.RESERVED);
            } else {
                slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            }
        }

        return slots;
    }

    @Transactional
    public void deleteSlot(Long id) {
        ParkingSlot slot = slotRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));

        slotRepo.deleteById(id);

        Long hubId = slot.getLot().getHub().getHubId();
        Long lotId = slot.getLot().getLotId();
        getSensorNodeRef(hubId, lotId).child(slot.getSlotCode()).removeValueAsync();
    }
}