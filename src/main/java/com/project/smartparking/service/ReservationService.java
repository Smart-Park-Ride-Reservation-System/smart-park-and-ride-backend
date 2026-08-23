package com.project.smartparking.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.smartparking.Config.AuthenticatedUser;
import com.project.smartparking.dto.GlobalSettingsDTO;
import com.project.smartparking.dto.ReservationDto;
import com.project.smartparking.dto.WalkInRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ReservationService {

    // Sri Lanka TimeZone Standard
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

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
    @Autowired
    private SlotStreamService slotStreamService;

    private LocalDateTime getSriLankaNow() {
        return ZonedDateTime.now(SRI_LANKA_ZONE).toLocalDateTime();
    }

    private boolean isSlotFree(ParkingSlot slot) {
        return slot.getStatus() == ParkingSlot.SlotStatus.AVAILABLE;
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getAll() {
        return reservationRepo.findAll().stream().map(ReservationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getByHub(Long hubId) {
        return reservationRepo.findByHubId(hubId).stream().map(ReservationDto::from).toList();
    }

    public Reservation getById(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));
    }

    public List<Reservation> getByUser(Long userId) {
        return reservationRepo.findByUser_UserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveReservationsForLot(Long hubId, Long lotId) {
        java.time.LocalDate today = getSriLankaNow().toLocalDate();
        return reservationRepo.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN || 
                             (r.getStatus() == ReservationStatus.CONFIRMED && 
                              r.getScheduledIn() != null && 
                              r.getScheduledIn().toLocalDate().equals(today)))
                .filter(r -> r.getSlot() != null
                        && r.getSlot().getLot() != null
                        && lotId.equals(r.getSlot().getLot().getLotId())
                        && r.getSlot().getLot().getHub() != null
                        && hubId.equals(r.getSlot().getLot().getHub().getHubId()))
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("reservationId", r.getReservationId());
                    m.put("slotCode", r.getSlot().getSlotCode());
                    m.put("vehicleNo", r.getVehicleNo());
                    m.put("ticketQrCode", r.getQrToken());
                    if (r.getActualIn() != null) {
                        m.put("entryTime", r.getActualIn().toString());
                    } else if (r.getScheduledIn() != null) {
                        m.put("entryTime", r.getScheduledIn().toString() + " (Scheduled)");
                    } else {
                        m.put("entryTime", null);
                    }
                    m.put("status", r.getStatus().name());
                    
                    // Calculate estimated fee
                    if (r.getActualIn() != null) {
                        // Store original actualOut
                        LocalDateTime originalOut = r.getActualOut();
                        try {
                            r.setActualOut(getSriLankaNow());
                            double estimatedFee = calculateParkingCharge(r);
                            m.put("estimatedFee", estimatedFee);
                        } finally {
                            // Restore it to avoid modifying the entity unexpectedly (even though it's read-only)
                            r.setActualOut(originalOut);
                        }
                    } else {
                        m.put("estimatedFee", 0.0);
                    }
                    
                    return m;
                })
                .toList();
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
    public synchronized Reservation createWalkIn(WalkInRequest request, Long attendantId) {
        ParkingSlot slot = slotRepo.findById(request.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid slotId"));

        assertHubAccess(slot.getSlotId());

        if (!isSlotFree(slot)) {
            throw new IllegalStateException("Slot is not free");
        }

        LocalDateTime slNow = getSriLankaNow();

        Reservation r = new Reservation();
        r.setSlot(slot);
        r.setUser(null);
        r.setCreatedByAttendantId(attendantId);
        r.setVehicleNo(request.getVehicleNo());
        r.setVehicleType(request.getVehicleType());
        r.setScheduledIn(slNow);
        r.setActualIn(slNow);
        r.setStatus(ReservationStatus.CHECKED_IN);
        r.setQrToken(UUID.randomUUID().toString());

        Reservation saved = reservationRepo.save(r);

        slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        slotRepo.save(slot);
        slotStreamService.notifySlotUpdate(slot);
        syncSlotToFirebase(slot, false);

        openGate(saved, "in");

        return saved;
    }

    @Transactional
    public synchronized Reservation create(Reservation r) {
        if (r.getSlot() == null || r.getSlot().getSlotId() == null)
            throw new IllegalArgumentException("slot is required");
        if (r.getUser() == null || r.getUser().getUserId() == null)
            throw new IllegalArgumentException("user is required");
        if (r.getScheduledIn() == null || r.getScheduledOut() == null)
            throw new IllegalArgumentException("scheduledIn and scheduledOut are required");

        Long slotId = r.getSlot().getSlotId();

        // Fetch slot record
        ParkingSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid slotId: " + slotId));

        LocalDateTime slNow = getSriLankaNow();

        // FIX 1: Set 5-minute soft-lock threshold for PENDING checkout entries to
        // prevent simultaneous bookings
        LocalDateTime pendingCutoff = slNow.minusMinutes(5);

        // 15-minute no-show cutoff window
        LocalDateTime noShowCutoff = slNow.minusMinutes(15);

        // FIX 2: Prevent concurrent bookings for the same time window
        List<Long> conflictingSlots = reservationRepo.findReservedSlotIdsInWindow(
                slot.getLot().getHub().getHubId(),
                r.getScheduledIn(),
                r.getScheduledOut(),
                pendingCutoff,
                noShowCutoff);

        if (conflictingSlots.contains(slotId)) {
            throw new IllegalStateException(
                    "This slot has just been selected or reserved by another user. Please choose another slot.");
        }

        r.setSlot(slot);
        r.setStatus(ReservationStatus.PENDING);
        r.setCreatedAt(slNow); // Guarantee Sri Lanka creation time for holding logic
        r.setReservationFee(BigDecimal.valueOf(200.0));

        // FIX 3: Prevent marking real-time slot as RESERVED when booking for future
        // dates (e.g. 23rd at 10 AM)
        boolean isImmediateBooking = !r.getScheduledIn().isAfter(slNow.plusMinutes(15));

        if (isImmediateBooking && slot.getStatus() == ParkingSlot.SlotStatus.AVAILABLE) {
            slot.setStatus(ParkingSlot.SlotStatus.RESERVED);
            slotRepo.save(slot);
            slotStreamService.notifySlotUpdate(slot);
            syncSlotToFirebase(slot, false);
        }

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
        
        Long slotId = r.getSlot().getSlotId();
        boolean wasActive = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN)
                .contains(r.getStatus());

        reservationRepo.deleteById(id);

        if (wasActive) {
            freeSlotIfNoActiveReservation(slotId);
        }
    }

    @Transactional
    public synchronized Reservation renewHold(Long id) {
        Reservation r = getById(id);
        if (r.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be renewed");
        }
        r.setCreatedAt(getSriLankaNow());
        return reservationRepo.save(r);
    }

    @Transactional
    public synchronized Reservation confirmPayment(Long id) {
        Reservation r = getById(id);

        // Explicitly expire stale PENDING reservations upon payment attempt
        if (r.getStatus() == ReservationStatus.PENDING
                && r.getCreatedAt().isBefore(getSriLankaNow().minusMinutes(5))) {
            expire(r);
            throw new IllegalStateException("Your 5-minute hold window has expired. Please select a slot again.");
        }

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
        if (r.getSlot() != null) { assertHubAccess(r.getSlot().getSlotId()); }
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Not in valid state for check-in: " + r.getStatus());
        }

        r.setActualIn(getSriLankaNow());
        r.setStatus(ReservationStatus.CHECKED_IN);

        if (r.getSlot() != null && r.getSlot().getSlotId() != null) { slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
            slotStreamService.notifySlotUpdate(slot);
            syncSlotToFirebase(slot, false);
        }); }

        openGate(r, "in");

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation checkOutByQrToken(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
        if (r.getSlot() != null) { assertHubAccess(r.getSlot().getSlotId()); }
        if (r.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Not in valid state for check-out: " + r.getStatus());
        }

        r.setActualOut(getSriLankaNow());
        r.setStatus(ReservationStatus.COMPLETED);

        if (r.getSlot() != null && r.getSlot().getSlotId() != null) { slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slotRepo.save(slot);
            slotStreamService.notifySlotUpdate(slot);
            syncSlotToFirebase(slot, false);
        }); }

        openGate(r, "out");

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation checkInById(Long id) {
        Reservation r = getById(id);
        if (r.getSlot() != null) { assertHubAccess(r.getSlot().getSlotId()); }
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Not in valid state for check-in: " + r.getStatus());
        }

        r.setActualIn(getSriLankaNow());
        r.setStatus(ReservationStatus.CHECKED_IN);

        if (r.getSlot() != null && r.getSlot().getSlotId() != null) { slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
            slotStreamService.notifySlotUpdate(slot);
            syncSlotToFirebase(slot, false);
        }); }

        openGate(r, "in");

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation checkOut(Long id) {
        Reservation r = getById(id);
        if (r.getSlot() != null) { assertHubAccess(r.getSlot().getSlotId()); }
        if (r.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Not checked in");
        }

        r.setActualOut(getSriLankaNow());
        r.setStatus(ReservationStatus.COMPLETED);

        if (r.getSlot() != null && r.getSlot().getSlotId() != null) { slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slotRepo.save(slot);
            slotStreamService.notifySlotUpdate(slot);
            syncSlotToFirebase(slot, false);
        }); }

        openGate(r, "out");

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
        if (r.getSlot() != null) { assertHubAccess(r.getSlot().getSlotId()); }

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

        if (r.getSlot() != null && r.getSlot().getSlotId() != null) {
            slotRepo.findById(r.getSlot().getSlotId()).ifPresent(slot -> {
                slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
                slotRepo.save(slot);
                slotStreamService.notifySlotUpdate(slot);
                syncSlotToFirebase(slot, false);
            });
        }
        openGate(r, "out");
        r.setStatus(ReservationStatus.PAID);
        return reservationRepo.save(r);
    }

    private double calculateParkingCharge(Reservation r) {
        if (r.getUser() != null && subscriptionService.getActiveSubscription(r.getUser().getUserId()) != null) {
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
            case "TUK":
            case "TUK TUK":
            case "THREE_WHEELER":
                rate = settings.getTukHourlyRate() != null ? settings.getTukHourlyRate() : 70.0;
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
        LocalDateTime slNow = getSriLankaNow();

        reservationRepo
                .findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, slNow.minusMinutes(5))
                .forEach(this::expire);

        LocalDateTime arrivalGraceThreshold = slNow.minusMinutes(15);

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
                    slotStreamService.notifySlotUpdate(slot);
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

        slotStreamService.notifySlotUpdate(slot);
        syncSlotToFirebase(slot, true);

        return slot;
    }

    private void openGate(Reservation r, String door) {
        if (r.getSlot() == null || r.getSlot().getLot() == null || r.getSlot().getLot().getHub() == null) {
            log.warn("Cannot open gate: missing slot/lot/hub for reservation {}", r.getReservationId());
            return;
        }
        Long hubId = r.getSlot().getLot().getHub().getHubId();
        Long lotId = r.getSlot().getLot().getLotId();
        String gatePath = "door/hubs/hub_" + hubId + "/lots/lot_" + lotId + "/" + door;
        
        DatabaseReference doorRef = FirebaseDatabase.getInstance().getReference(gatePath);
        doorRef.setValueAsync("open");
    }

    public void openGateManually(Long hubId, Long lotId, String door) {
        // Enforce hub admin boundaries
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser current) {
            if ("HUB_ADMIN".equals(current.role()) && !hubId.equals(current.hubId())) {
                throw new AccessDeniedException("You can only open gates for your assigned hub.");
            }
        }
        
        if (!"in".equals(door) && !"out".equals(door)) {
            throw new IllegalArgumentException("Door must be 'in' or 'out'");
        }

        String gatePath = "door/hubs/hub_" + hubId + "/lots/lot_" + lotId + "/" + door;
        DatabaseReference doorRef = FirebaseDatabase.getInstance().getReference(gatePath);
        doorRef.setValueAsync("open");
        log.info("Manual gate open requested: hub={}, lot={}, door={}", hubId, lotId, door);
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



