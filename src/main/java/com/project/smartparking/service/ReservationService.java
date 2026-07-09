package com.project.smartparking.service;

import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.Payment;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.PaymentRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepo;
    @Autowired
    private PaymentRepository paymentRepo;
    @Autowired
    private ParkingSlotRepository slotRepo;

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

    public Reservation confirmPayment(Long id) {
        Reservation r = getById(id);

        if (!"PENDING".equals(r.getStatus())) {
            throw new IllegalStateException("Only PENDING can be confirmed");
        }

        r.setStatus("CONFIRMED");
        r.setQrToken(UUID.randomUUID().toString());

        return reservationRepo.save(r);
    }

    public Reservation checkInByQrToken(String qrToken) {
        Reservation r = reservationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));

        if (!"CONFIRMED".equals(r.getStatus())) {
            throw new IllegalStateException("Not in valid state for check-in: " + r.getStatus());
        }

        r.setActualIn(LocalDateTime.now());
        r.setStatus("CHECKED_IN");

        slotRepo.findById(r.getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
        });

        return reservationRepo.save(r);
    }

    public Reservation checkInById(Long id) {
        Reservation r = getById(id);

        if (!List.of("CONFIRMED", "PENDING").contains(r.getStatus())) {
            throw new IllegalStateException("Not in valid state for check-in");
        }

        r.setActualIn(LocalDateTime.now());
        r.setStatus("CHECKED_IN");

        slotRepo.findById(r.getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slotRepo.save(slot);
        });

        return reservationRepo.save(r);
    }

    public Reservation checkOut(Long id) {
        Reservation r = getById(id);

        if (!"CHECKED_IN".equals(r.getStatus())) {
            throw new IllegalStateException("Not checked in");
        }

        r.setActualOut(LocalDateTime.now());
        r.setStatus("COMPLETED");

        slotRepo.findById(r.getSlotId()).ifPresent(slot -> {
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slotRepo.save(slot);
        });

        return reservationRepo.save(r);
    }

    public Reservation markPaid(Long id) {
        Reservation r = getById(id);

        if (!"COMPLETED".equals(r.getStatus())) {
            throw new IllegalStateException("Only COMPLETED reservations can be marked as paid");
        }

        double calculatedAmount = calculateParkingCharge(r);

        Payment payment = new Payment();
        payment.setReservationId(r.getReservationId());
        payment.setAmount(BigDecimal.valueOf(calculatedAmount));
        payment.setPaymentMethod("CASH");
        payment.setPaymentStatus("SUCCESS");

        paymentRepo.save(payment);

        r.setStatus("PAID");
        return reservationRepo.save(r);
    }

    private double calculateParkingCharge(Reservation r) {
        // Proportional per-minute billing (NOT rounded up to the full hour).
        // Must stay in sync with the frontend's calcAmount() in dashboard/page.tsx —
        // both compute hours = elapsedMillis / (1000*60*60), then hours * rate,
        // rounded up to the nearest cent (not the nearest hour).
        long millis = java.time.Duration.between(r.getActualIn(), r.getActualOut()).toMillis();
        double hours = millis / (1000.0 * 60.0 * 60.0);

        double rate;
        switch (r.getVehicleType().toUpperCase()) {
            case "BIKE":
                rate = 50;
                break;
            case "CAR":
                rate = 100;
                break;
            case "SUV":
                rate = 130;
                break;
            case "VAN":
                rate = 150;
                break;
            default:
                rate = 100;
        }

        double amount = hours * rate;
        return Math.ceil(amount * 100) / 100.0; // round to nearest cent, same as frontend
    }

    @Scheduled(fixedRate = 30000)
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();

        // Grace period is measured from the user's SCHEDULED slot time (scheduledIn),
        // not from when the reservation record was created (bookingDate).
        // A reservation made well ahead of time for a future slot must not expire
        // before that slot's actual start time + grace period arrives.
        reservationRepo
                .findByStatusAndScheduledInBeforeAndActualInIsNull("PENDING", now.minusMinutes(15))
                .forEach(this::expire);

        reservationRepo
                .findByStatusAndScheduledInBeforeAndActualInIsNull("CONFIRMED", now.minusMinutes(15))
                .forEach(this::expire);
    }

    @Scheduled(fixedRate = 60000)
    public void autoCancelUnoccupiedReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> overdueReservations = reservationRepo.findByStatusAndScheduledInBeforeAndActualInIsNull(
                "CONFIRMED",
                now.minusMinutes(15));

        for (Reservation reservation : overdueReservations) {
            reservation.setStatus("CANCELLED");
            reservationRepo.save(reservation);

            slotRepo.findById(reservation.getSlotId()).ifPresent(slot -> {
                slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
                slotRepo.save(slot);
            });

        }
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
            });
        }
    }
}