package com.project.smartparking.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.smartparking.Config.AuthenticatedUser;
import com.project.smartparking.dto.GlobalSettingsDTO;
import com.project.smartparking.dto.ReservationDto;
import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.Payment;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.model.Reservation.ReservationStatus;
import com.project.smartparking.repository.ParkingLotRepository;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.PaymentRepository;
import com.project.smartparking.repository.ReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
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

    @Transactional
    public List<ReservationDto> getAll() {
        return reservationRepo.findAll().stream().map(ReservationDto::from).toList();
    }

    public Reservation getById(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));
    }

    public List<Reservation> getByUser(Long userId) {
        // Aligned with updated repository method name: findByUser_UserId
        return reservationRepo.findByUser_UserId(userId);
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
        ParkingLot lot = lotRepo.findById(slot.getLot().getLotId())
                .orElseThrow(() -> new IllegalStateException("Slot references a missing lot"));

        if (!current.hubId().equals(lot.getHub().getHubId())) {
            throw new AccessDeniedException("You are not assigned to this hub");
        }
    }

    @Transactional
    public Reservation create(Reservation r) {
        if (r.getSlot() == null || r.getSlot().getSlotId() == null)
            throw new IllegalArgumentException("slot is required");
        if (r.getUser() == null || r.getUser().getUserId() == null)
            throw new IllegalArgumentException("user is required");
        if (r.getScheduledIn() == null || r.getScheduledOut() == null)
            throw new IllegalArgumentException("scheduledIn and scheduledOut are required");

        Long slotId = r.getSlot().getSlotId();
        ParkingSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid slotId: " + slotId));

        // Check if slot is reserved in the given time frame
        List<Long> conflictingSlots = reservationRepo.findReservedSlotIdsInWindow(
                slot.getLot().getHub().getHubId(),
                r.getScheduledIn(),
                r.getScheduledOut());

        if (conflictingSlots.contains(slotId)) {
            throw new IllegalStateException("This slot is already reserved for the selected time window.");
        }

        r.setSlot(slot);
        r.setStatus(ReservationStatus.PENDING);
        r.setReservationFee(BigDecimal.valueOf(200.0));

        slot.setStatus(ParkingSlot.SlotStatus.RESERVED);
        slotRepo.save(slot);

        // Non-blocking Firebase update recommended
        syncSlotToFirebase(slot, false);

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation update(Long id, Reservation updated) {
        Reservation r = getById(id);

        if (List.of(ReservationStatus.COMPLETED, ReservationStatus.CANCELLED, ReservationStatus.EXPIRED)
                .contains(r.getStatus())) {
            throw new IllegalStateException("Cannot edit a " + r.getStatus() + " reservation.");
        }

        r.setVehicleNo(updated.getVehicleNo());
        r.setVehicleType(updated.getVehicleType());
        r.setScheduledIn(updated.getScheduledIn());

        return reservationRepo.save(r);
    }

    @Transactional
    public void cancel(Long id) {
        Reservation r = getById(id);

        if (List.of(ReservationStatus.COMPLETED, ReservationStatus.CANCELLED, ReservationStatus.EXPIRED)
                .contains(r.getStatus())) {
            throw new IllegalStateException("Already " + r.getStatus());
        }
        if (r.getStatus() == ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot cancel after check-in");
        }

        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepo.save(r);
        freeSlotIfNoActiveReservation(r.getSlot().getSlotId());
    }

    @Transactional
    public void delete(Long id) {
        Reservation r = getById(id);

        if (List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN)
                .contains(r.getStatus())) {
            freeSlotIfNoActiveReservation(r.getSlot().getSlotId());
        }

        reservationRepo.deleteById(id);
    }

    @Transactional
    public synchronized Reservation confirmPayment(Long id) {
        Reservation r = getById(id);

        if (List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.COMPLETED)
                .contains(r.getStatus())) {
            return r;
        }

        if (r.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }

        if (r.getReservationFee() == null) {
            throw new IllegalStateException("Reservation fee is not set");
        }

        boolean reservationFeeAlreadyPaid = paymentRepo.findByReservationReservationId(r.getReservationId())
                .stream()
                .anyMatch(p -> Payment.PaymentMethod.CARD == p.getPaymentMethod()
                        && Payment.PaymentStatus.SUCCESS == p.getPaymentStatus());

        if (!reservationFeeAlreadyPaid) {
            Payment payment = Payment.builder()
                    .reservation(r)
                    .amount(r.getReservationFee())
                    .paymentMethod(Payment.PaymentMethod.CARD)
                    .paymentStatus(Payment.PaymentStatus.SUCCESS)
                    .build();
            paymentRepo.save(payment);
        }

        r.setStatus(ReservationStatus.CONFIRMED);
        if (r.getQrToken() == null) {
            r.setQrToken(UUID.randomUUID().toString());
        }

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation checkInByQrToken(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
        assertHubAccess(r.getSlot().getSlotId());
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Not in valid state for check-in: " + r.getStatus());
        }

        r.setActualIn(LocalDateTime.now());
        r.setStatus(ReservationStatus.CHECKED_IN);

        slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        openGate("in");

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation checkOutByQrToken(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
        assertHubAccess(r.getSlot().getSlotId());
        if (r.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Not in valid state for check-out: " + r.getStatus());
        }

        r.setActualOut(LocalDateTime.now());
        r.setStatus(ReservationStatus.COMPLETED);

        slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        openGate("out");

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation checkInById(Long id) {
        Reservation r = getById(id);
        assertHubAccess(r.getSlot().getSlotId());
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Not in valid state for check-in: " + r.getStatus());
        }

        r.setActualIn(LocalDateTime.now());
        r.setStatus(ReservationStatus.CHECKED_IN);

        slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        openGate("in");

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation checkOut(Long id) {
        Reservation r = getById(id);
        assertHubAccess(r.getSlot().getSlotId());
        if (r.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Not checked in");
        }

        r.setActualOut(LocalDateTime.now());
        r.setStatus(ReservationStatus.COMPLETED);

        slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slotRepo.save(slot);
            syncSlotToFirebase(slot, false);
        });

        openGate("out");

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation processQrScan(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));

        return switch (r.getStatus()) {
            case CONFIRMED -> checkInByQrToken(qrToken);
            case CHECKED_IN -> checkOutByQrToken(qrToken);
            default -> throw new IllegalStateException("Cannot process QR code for status: " + r.getStatus());
        };
    }

    @Transactional
    public synchronized Reservation markPaid(Long id) {
        Reservation r = getById(id);
        assertHubAccess(r.getSlot().getSlotId());

        if (r.getStatus() != ReservationStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED reservations can be marked as paid");
        }

        boolean durationChargeAlreadyPaid = paymentRepo.findByReservationReservationId(r.getReservationId())
                .stream()
                .anyMatch(p -> Payment.PaymentMethod.CASH == p.getPaymentMethod()
                        && Payment.PaymentStatus.SUCCESS == p.getPaymentStatus());

        if (!durationChargeAlreadyPaid) {
            double calculatedAmount = calculateParkingCharge(r);

            Payment payment = Payment.builder()
                    .reservation(r)
                    .amount(BigDecimal.valueOf(calculatedAmount))
                    .paymentMethod(Payment.PaymentMethod.CASH)
                    .paymentStatus(Payment.PaymentStatus.SUCCESS)
                    .build();

            paymentRepo.save(payment);
        }

        return reservationRepo.save(r);
    }

    private double calculateParkingCharge(Reservation r) {
        if (subscriptionService.getActiveSubscription(r.getUser().getUserId()) != null) {
            return 0.0;
        }

        long minutes = java.time.Duration.between(r.getActualIn(), r.getActualOut()).toMinutes();
        GlobalSettingsDTO settings = globalSettingsService.getSettings();

        if (settings.getEntryGracePeriodMinutes() != null && minutes <= settings.getEntryGracePeriodMinutes()) {
            return 0.0;
        }

        double hours = minutes / 60.0;
        double rate;
        String vehicleType = r.getVehicleType() != null ? r.getVehicleType().toUpperCase() : "CAR";
        switch (vehicleType) {
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
            if (amount > cap)
                amount = cap;
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
    @Transactional
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();

        // Updated status parameter to ReservationStatus enum
        reservationRepo
                .findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, now.minusMinutes(10))
                .forEach(this::expire);

        LocalDateTime arrivalGraceThreshold = now.minusMinutes(15);

        // Updated status parameter to ReservationStatus enum
        reservationRepo
                .findByStatusAndScheduledInBeforeAndActualInIsNull(ReservationStatus.CONFIRMED, arrivalGraceThreshold)
                .forEach(this::expire);
    }

    private void expire(Reservation r) {
        r.setStatus(ReservationStatus.EXPIRED);
        reservationRepo.save(r);
        log.info("[ReservationService] Reservation {} for slot {} expired", r.getReservationId(),
                r.getSlot().getSlotId());
        freeSlotIfNoActiveReservation(r.getSlot().getSlotId());
    }

    private void freeSlotIfNoActiveReservation(Long slotId) {
        // Updated query call to pass ReservationStatus enums
        boolean stillActive = reservationRepo
                .findBySlotSlotIdAndStatusIn(slotId,
                        List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN))
                .stream()
                .anyMatch(res -> res.getActualOut() == null);

        if (!stillActive) {
            slotRepo.findById(slotId).ifPresent(slot -> {
                if (slot.getStatus() == ParkingSlot.SlotStatus.OCCUPIED) {
                    log.info(
                            "[ReservationService] Reservation ended for slot {}, but vehicle is physically present. Keeping status OCCUPIED.",
                            slotId);
                    return;
                }

                if (slot.getStatus() != ParkingSlot.SlotStatus.OUT_OF_SERVICE) {
                    slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
                    slotRepo.save(slot);
                    syncSlotToFirebase(slot, false);
                    log.info("[ReservationService] Slot {} freed and marked AVAILABLE", slotId);
                }
            });
        }
    }

    @Transactional
    public ParkingSlot setSlotStatusManually(Long slotId, ParkingSlot.SlotStatus status) {
        assertHubAccess(slotId);
        ParkingSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid slotId"));

        slot.setStatus(status);
        slotRepo.save(slot);
        syncSlotToFirebase(slot, true);

        return slot;
    }

    private void openGate(String door) {
        DatabaseReference doorRef = FirebaseDatabase.getInstance()
                .getReference("door/" + door);
        doorRef.setValueAsync("open");
    }

    private void syncSlotToFirebase(ParkingSlot slot, boolean manualOverride) {
        DatabaseReference slotRef = FirebaseDatabase.getInstance()
                .getReference("slots/" + slot.getSlotId());

        Map<String, Object> update = new HashMap<>();
        update.put("status", slot.getStatus().name());
        update.put("manualOverride", manualOverride);
        update.put("updatedAt", Instant.now().toString());

        slotRef.updateChildrenAsync(update);
    }

    
}