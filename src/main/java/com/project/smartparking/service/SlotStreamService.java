package com.project.smartparking.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SlotStreamService {

    private static final Logger log = LoggerFactory.getLogger(SlotStreamService.class);

    @Autowired
    private ParkingSlotRepository slotRepo;

    @Autowired
    private ReservationRepository reservationRepo;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByLot = new ConcurrentHashMap<>();
    private final Map<String, ValueEventListener> listenersByLot = new ConcurrentHashMap<>();
    private final Map<String, DatabaseReference> refsByLot = new ConcurrentHashMap<>();
    private final Map<String, List<SlotStateDto>> lastStateByLot = new ConcurrentHashMap<>();

    
    private final Map<String, Map<String, Boolean>> firebaseOccupancyByLot = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long hubId, Long lotId) {
        String key = key(hubId, lotId);
        SseEmitter emitter = new SseEmitter(0L);

        emittersByLot.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(hubId, lotId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        if (!listenersByLot.containsKey(key)) {
            attachFirebaseListener(hubId, lotId, key);
        } else {
            List<SlotStateDto> cached = lastStateByLot.get(key);
            if (cached != null) {
                sendSafely(emitter, cached);
            }
        }

        return emitter;
    }

    public void notifySlotUpdate(ParkingSlot slot) {
        if (slot != null && slot.getLot() != null) {
            Long lotId = slot.getLot().getLotId();
            Long hubId = (slot.getLot().getHub() != null) ? slot.getLot().getHub().getHubId() : null;
            if (hubId != null) {
                notifyLotChange(hubId, lotId);
            }
        }
    }

    public void notifySlotUpdate(Long hubId, ParkingSlot slot) {
        if (slot != null && slot.getLot() != null) {
            notifyLotChange(hubId, slot.getLot().getLotId());
        }
    }

   
    public void notifyLotChange(Long hubId, Long lotId) {
        String key = key(hubId, lotId);
        if (!emittersByLot.containsKey(key)) {
            return; 
        }

        Map<String, Boolean> occupancy = firebaseOccupancyByLot.getOrDefault(key, Collections.emptyMap());
        List<SlotStateDto> states = computeSlotStates(hubId, lotId, occupancy);
        lastStateByLot.put(key, states);
        pushToLot(key, states);
    }

    private void attachFirebaseListener(Long hubId, Long lotId, String key) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("sensor")
                .child("hubs").child("hub_" + hubId)
                .child("lots").child("lot_" + lotId);
        refsByLot.put(key, ref);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Boolean> occupancy = new HashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean occupied = child.child("occupied").getValue(Boolean.class);
                    occupancy.put(child.getKey(), occupied != null && occupied);
                }

                // Cache firebase state
                firebaseOccupancyByLot.put(key, occupancy);

                List<SlotStateDto> states = computeSlotStates(hubId, lotId, occupancy);
                lastStateByLot.put(key, states);
                pushToLot(key, states);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Firebase listener cancelled for lot stream {}: {}", key, error.getMessage());
            }
        };

        ref.addValueEventListener(listener);
        listenersByLot.put(key, listener);
        log.info("Attached Firebase listener for lot stream {}", key);
    }

    private List<SlotStateDto> computeSlotStates(Long hubId, Long lotId, Map<String, Boolean> occupancy) {
        List<ParkingSlot> slots = slotRepo.findByLot_LotId(lotId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now;
        LocalDateTime end = start.plusHours(2);
        LocalDateTime pendingCutoff = now.minusMinutes(10);
        LocalDateTime noShowCutoff = now.minusMinutes(15);

        List<Long> reservedSlotIds = reservationRepo.findReservedSlotIdsInWindow(
                hubId, start, end, pendingCutoff, noShowCutoff);

        List<SlotStateDto> result = new ArrayList<>();
        for (ParkingSlot slot : slots) {
            ParkingSlot.SlotStatus status;

            if (slot.getStatus() == ParkingSlot.SlotStatus.OUT_OF_SERVICE) {
                status = ParkingSlot.SlotStatus.OUT_OF_SERVICE;
            }
            else if (occupancy.getOrDefault(slot.getSlotCode(), false)
                    || slot.getStatus() == ParkingSlot.SlotStatus.OCCUPIED) {
                status = ParkingSlot.SlotStatus.OCCUPIED;
            }
            else if (reservedSlotIds.contains(slot.getSlotId())
                    || slot.getStatus() == ParkingSlot.SlotStatus.RESERVED) {
                status = ParkingSlot.SlotStatus.RESERVED;
            }
            else {
                status = ParkingSlot.SlotStatus.AVAILABLE;
            }
            result.add(new SlotStateDto(slot.getSlotId(), slot.getSlotCode(), status));
        }
        return result;
    }

    private void pushToLot(String key, List<SlotStateDto> states) {
        List<SseEmitter> subs = emittersByLot.get(key);
        if (subs == null)
            return;
        for (SseEmitter emitter : subs) {
            sendSafely(emitter, states);
        }
    }

    private void sendSafely(SseEmitter emitter, List<SlotStateDto> states) {
        try {
            emitter.send(SseEmitter.event().name("slots").data(states));
        } catch (Exception e) {
            emitter.complete();
        }
    }

    private void removeEmitter(Long hubId, Long lotId, SseEmitter emitter) {
        String key = key(hubId, lotId);
        List<SseEmitter> subs = emittersByLot.get(key);
        if (subs == null)
            return;
        subs.remove(emitter);

        if (subs.isEmpty()) {
            emittersByLot.remove(key);
            firebaseOccupancyByLot.remove(key);
            ValueEventListener listener = listenersByLot.remove(key);
            DatabaseReference ref = refsByLot.remove(key);
            lastStateByLot.remove(key);
            if (listener != null && ref != null) {
                ref.removeEventListener(listener);
                log.info("Detached Firebase listener for lot stream {}", key);
            }
        }
    }

    private String key(Long hubId, Long lotId) {
        return hubId + ":" + lotId;
    }

    public record SlotStateDto(Long slotId, String slotCode, ParkingSlot.SlotStatus status) {
    }
}