package com.project.smartparking.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.Payment;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.PaymentRepository;
import com.project.smartparking.repository.ReservationRepository;
import com.project.smartparking.dto.GlobalSettingsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.project.smartparking.Config.AuthenticatedUser;
import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.repository.ParkingLotRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepo;
    @Autowired
    private PaymentRepository paymentRepo;
    @Autowired
    private ParkingSlotRepository slotRepo;
    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private GlobalSettingsService globalSettingsService;
    @Autowired
    private ParkingLotRepository lotRepo;

    public List<Reservation> getAll() {
        return reservationRepo.findAll();
    }

    public Reservation getById(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));
    }

    public List<Reservation> getByUser(Long userId) {
        return reservationRepo.findByUserId(userId);
    }

    private void assertHubAccess(Long slotId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser current)) {
            return;
        }
        if ("ADMIN".equals(current.role())) {
            return;
        }
        if (current.hubId() == null) {
            throw new AccessDeniedException("Your account has no hub assigned");
        }

        ParkingSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid slotId"));
        ParkingLot lot = lotRepo.findById(slot.getLotId())
                .orElseThrow(() -> new IllegalStateException("Slot references a missing lot"));

        if (!current.hubId().equals(lot.getHub().getHubId())) {
            throw new AccessDeniedException("You are not assigned to this hub");
        }
    }

    public Reservation create(Reservation r) {
        if (r.getSlotId() == null)
            throw new IllegalArgumentException("slotId is required");
        if (r.getUserId() == null)
            throw new IllegalArgumentException("userId is required");
        if (r.getScheduledIn() == null)
            throw new IllegalArgumentException("scheduledIn is required");

        ParkingSlot slot = slotRepo.findById(r.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid slotId"));

        boolean alreadyBooked = reservationRepo
                .findBySlotIdAndStatusIn(r.getSlotId(),
                        List.of("PENDING", "CONFIRMED", "CHECKED_IN"))
                .stream()
                .anyMatch(res -> res.getActualOut() == null);

        if (alreadyBooked)
            throw new IllegalStateException("This slot is already reserved.");

        r.setStatus("PENDING");
        r.setReservationFee(200.0);
        slot.setStatus(ParkingSlot.SlotStatus.RESERVED);
        slotRepo.save(slot);
        syncSlotToFirebase(slot, false);

        return reservationRepo.save(r);
    }

    public Reservation update(Long id, Reservation updated) {
        Reservation r = getById(id);

        if (List.of("COMPLETED", "EXPIRED", "CANCELLED", "PAID").contains(r.getStatus())) {
            throw new IllegalStateException("Cannot edit a " + r.getStatus() + " reservation.");
        }

        r.setVehicleNo(updated.getVehicleNo());
        r.setVehicleType(updated.getVehicleType());
        r.setReservationType(updated.getReservationType());
        r.setScheduledIn(updated.getScheduledIn());

        return reservationRepo.save(r);
    }

    public void cancel(Long id) {
        Reservation r = getById(id);

        if (List.of("COMPLETED", "EXPIRED", "CANCELLED", "PAID").contains(r.getStatus())) {
            throw new IllegalStateException("Already " + r.getStatus());
        }
        if ("CHECKED_IN".equals(r.getStatus())) {
            throw new IllegalStateException("Cannot cancel after check-in");
        }

        r.setStatus("CANCELLED");
        reservationRepo.save(r);
        freeSlotIfNoActiveReservation(r.getSlotId());
    }

    public void delete(Long id) {
        Reservation r = getById(id);

        if (List.of("PENDING", "CONFIRMED", "CHECKED_IN").contains(r.getStatus())) {
            freeSlotIfNoActiveReservation(r.getSlotId());
        }

        reservationRepo.deleteById(id);
    }

    @Transactional
    public synchronized Reservation confirmPayment(Long id) {
        Reservation r = getById(id);

        if (List.of("CONFIRMED", "CHECKED_IN", "COMPLETED", "PAID").contains(r.getStatus())) {
            return r;
        }

        if (!"PENDING".equals(r.getStatus())) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }

        if (r.getReservationFee() == null) {
            throw new IllegalStateException("Reservation fee is not set");
        }

        boolean reservationFeeAlreadyPaid = paymentRepo.findByReservationId(r.getReservationId())
                .stream()
                .anyMatch(p -> "CARD".equals(p.getPaymentMethod()) && "SUCCESS".equals(p.getPaymentStatus()));

        if (!reservationFeeAlreadyPaid) {
            Payment payment = new Payment();
            payment.setReservationId(r.getReservationId());
            payment.setAmount(BigDecimal.valueOf(r.getReservationFee()));
            payment.setPaymentMethod("CARD");
            payment.setPaymentStatus("SUCCESS");
            paymentRepo.save(payment);
        }

        r.setStatus("CONFIRMED");
        if (r.getQrToken() == null) {
            r.setQrToken(UUID.randomUUID().toString());
        }

        return reservationRepo.save(r);
    }

    public Reservation checkInByQrToken(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
        assertHubAccess(r.getSlotId());
        if (!"CONFIRMED".equals(r.getStatus())) {
            throw new IllegalStateException("Not in valid state for check-in: " + r.getStatus());
        }

        r.setActualIn(LocalDateTime.now());
        r.setStatus("CHECKED_IN");

        slotRepo.findById(r.getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        openGate("entry-gate");

        return reservationRepo.save(r);
    }

    public Reservation checkInById(Long id) {
        Reservation r = getById(id);
        assertHubAccess(r.getSlotId());
        if (!"CONFIRMED".equals(r.getStatus())) {
            throw new IllegalStateException("Not in valid state for check-in: " + r.getStatus());
        }

        r.setActualIn(LocalDateTime.now());
        r.setStatus("CHECKED_IN");

        slotRepo.findById(r.getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        return reservationRepo.save(r);
    }

    public Reservation checkOut(Long id) {
        Reservation r = getById(id);
        assertHubAccess(r.getSlotId());
        if (!"CHECKED_IN".equals(r.getStatus())) {
            throw new IllegalStateException("Not checked in");
        }

        r.setActualOut(LocalDateTime.now());
        r.setStatus("COMPLETED");

        slotRepo.findById(r.getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        return reservationRepo.save(r);
    }

    @Transactional
    public synchronized Reservation markPaid(Long id) {
        Reservation r = getById(id);
        assertHubAccess(r.getSlotId());
        if ("PAID".equals(r.getStatus())) {
            return r;
        }

        if (!"COMPLETED".equals(r.getStatus())) {
            throw new IllegalStateException("Only COMPLETED reservations can be marked as paid");
        }

        boolean durationChargeAlreadyPaid = paymentRepo.findByReservationId(r.getReservationId())
                .stream()
                .anyMatch(p -> "CASH".equals(p.getPaymentMethod()) && "SUCCESS".equals(p.getPaymentStatus()));

        if (!durationChargeAlreadyPaid) {
            double calculatedAmount = calculateParkingCharge(r);

            Payment payment = new Payment();
            payment.setReservationId(r.getReservationId());
            payment.setAmount(BigDecimal.valueOf(calculatedAmount));
            payment.setPaymentMethod("CASH");
            payment.setPaymentStatus("SUCCESS");

            paymentRepo.save(payment);
        }

        r.setStatus("PAID");
        return reservationRepo.save(r);
    }

    private double calculateParkingCharge(Reservation r) {

        if (subscriptionService.getActiveSubscription(r.getUserId()) != null) {
            return 0.0;
        }

        long minutes = java.time.Duration.between(r.getActualIn(), r.getActualOut()).toMinutes();

        GlobalSettingsDTO settings = globalSettingsService.getSettings();

        if (settings.getEntryGracePeriodMinutes() != null && minutes <= settings.getEntryGracePeriodMinutes()) {
            return 0.0;
        }

        double hours = minutes / 60.0;

        double rate;
        switch (r.getVehicleType().toUpperCase()) {
            case "BIKE":
                rate = settings.getBikeHourlyRate() != null ? settings.getBikeHourlyRate() : 50.0;
                break;
            case "CAR":
                rate = settings.getCarHourlyRate() != null ? settings.getCarHourlyRate() : 100.0;
                break;
            case "SUV":
                rate = settings.getSuvHourlyRate() != null ? settings.getSuvHourlyRate() : 130.0;
                break;
            case "VAN":
                rate = settings.getVanHourlyRate() != null ? settings.getVanHourlyRate() : 150.0;
                break;
            default:
                rate = settings.getCarHourlyRate() != null ? settings.getCarHourlyRate() : 100.0;
        }

        double amount = hours * rate;

        if (settings.getDailyMaximumCap() != null && settings.getDailyMaximumCap() > 0) {
            double days = Math.ceil(hours / 24.0);
            if (days == 0)
                days = 1;
            double cap = days * settings.getDailyMaximumCap();
            if (amount > cap) {
                amount = cap;
            }
        }

        if (settings.getMaximumParkingDurationHours() != null && settings.getMaximumParkingDurationHours() > 0) {
            if (hours > settings.getMaximumParkingDurationHours()) {
                double overstayHours = hours - settings.getMaximumParkingDurationHours();
                double penaltyRate = settings.getOverstayPenaltyRate() != null ? settings.getOverstayPenaltyRate()
                        : rate * 2;
                amount += overstayHours * penaltyRate;
            }
        }

        return Math.ceil(amount * 100) / 100.0;
    }

    @Scheduled(fixedRate = 30000)
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();

        reservationRepo
                .findByStatusAndScheduledInBeforeAndActualInIsNull("PENDING", now.minusMinutes(10))
                .forEach(this::expire);

        reservationRepo
                .findByStatusAndScheduledInBeforeAndActualInIsNull("CONFIRMED", now.minusMinutes(15))
                .forEach(this::expire);
    }

    private void expire(Reservation r) {
        r.setStatus("EXPIRED");
        reservationRepo.save(r);
        freeSlotIfNoActiveReservation(r.getSlotId());
    }

    private void freeSlotIfNoActiveReservation(Long slotId) {
        boolean stillActive = reservationRepo
                .findBySlotIdAndStatusIn(slotId,
                        List.of("PENDING", "CONFIRMED", "CHECKED_IN"))
                .stream()
                .anyMatch(res -> res.getActualOut() == null);

        if (!stillActive) {
            slotRepo.findById(slotId).ifPresent(slot -> {
                slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
                slotRepo.save(slot);
                syncSlotToFirebase(slot, false);
            });
        }
    }

    public Reservation checkOutByQrToken(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
        assertHubAccess(r.getSlotId());
        if (!"CHECKED_IN".equals(r.getStatus())) {
            throw new IllegalStateException("Not in valid state for check-out: " + r.getStatus());
        }

        r.setActualOut(LocalDateTime.now());
        r.setStatus("COMPLETED");

        slotRepo.findById(r.getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        openGate("exit-gate");

        return reservationRepo.save(r);
    }

    public Reservation processQrScan(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
        
        return switch (r.getStatus()) {
            case "CONFIRMED" -> checkInByQrToken(qrToken);
            case "CHECKED_IN" -> checkOutByQrToken(qrToken);
            case "COMPLETED", "PAID" -> throw new IllegalStateException("Already checked out.");
            case "EXPIRED", "CANCELLED" ->
                throw new IllegalStateException("Reservation is no longer valid: " + r.getStatus());
            default -> throw new IllegalStateException("Unexpected status: " + r.getStatus());
        };
    }

    public ParkingSlot setSlotStatusManually(Long slotId, ParkingSlot.SlotStatus status) {
        assertHubAccess(slotId);
        ParkingSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid slotId"));

        slot.setStatus(status);
        slotRepo.save(slot);
        syncSlotToFirebase(slot, true);

        return slot;
    }

    private void openGate(String gateId) {
        DatabaseReference gateRef = FirebaseDatabase.getInstance()
                .getReference("gates/" + gateId + "/open");
        gateRef.setValueAsync(true);
    }

    private void syncSlotToFirebase(ParkingSlot slot, boolean manualOverride) {
        DatabaseReference slotRef = FirebaseDatabase.getInstance()
                .getReference("slots/" + slot.getSlotId());

        Map<String, Object> update = new HashMap<>();
        update.put("status", slot.getStatus().name()); // e.g. AVAILABLE / RESERVED / OCCUPIED
        update.put("manualOverride", manualOverride);
        update.put("updatedAt", Instant.now().toString());

        slotRef.updateChildrenAsync(update);
    }
}