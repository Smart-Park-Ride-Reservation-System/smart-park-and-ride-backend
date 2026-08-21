package com.project.smartparking.service;

import com.project.smartparking.model.Payment;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.PaymentRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }

    public List<Payment> getByHub(Long hubId) {
        return paymentRepository.findByHubId(hubId);
    }

    public Payment getById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    public List<Payment> getByReservation(Long reservationId) {
        return paymentRepository.findByReservationReservationId(reservationId);
    }

    @Transactional
    public Payment create(Payment payment) {
        if (payment.getReservation() == null || payment.getReservation().getReservationId() == null) {
            throw new IllegalArgumentException("reservation is required");
        }

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be a positive value");
        }

        Reservation reservation = reservationRepository.findById(payment.getReservation().getReservationId())
                .orElseThrow(() -> new RuntimeException(
                        "Reservation not found: " + payment.getReservation().getReservationId()));
        payment.setReservation(reservation);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment updateStatus(Long id, String status) {
        Payment.PaymentStatus enumStatus = parseEnum(Payment.PaymentStatus.class, status,
                "Invalid payment status: " + status);

        Payment payment = getById(id);
        payment.setPaymentStatus(enumStatus);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment update(Long id, Payment updated) {
        Payment payment = getById(id);

        if (updated.getReservation() == null || updated.getReservation().getReservationId() == null) {
            throw new IllegalArgumentException("reservation is required");
        }

        if (updated.getAmount() == null || updated.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be a positive value");
        }

        Reservation reservation = reservationRepository.findById(updated.getReservation().getReservationId())
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        payment.setReservation(reservation);
        payment.setAmount(updated.getAmount());
        payment.setPaymentMethod(updated.getPaymentMethod());
        payment.setPaymentStatus(updated.getPaymentStatus());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment recordPayment(Long reservationId, Double amount, String method) {
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId is required");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be a positive value");
        }

        Payment.PaymentMethod resolvedMethod = (method != null)
                ? parseEnum(Payment.PaymentMethod.class, method, "Invalid payment method: " + method)
                : Payment.PaymentMethod.CASH;

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        Payment duplicate = paymentRepository.findByReservationReservationId(reservationId)
                .stream()
                .filter(p -> resolvedMethod == p.getPaymentMethod()
                        && Payment.PaymentStatus.SUCCESS == p.getPaymentStatus())
                .findFirst()
                .orElse(null);

        if (duplicate != null) {
            return duplicate;
        }

        Payment payment = Payment.builder()
                .reservation(reservation)
                .amount(BigDecimal.valueOf(amount))
                .paymentMethod(resolvedMethod)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .build();

        return paymentRepository.save(payment);
    }

    public Map<String, BigDecimal> getRevenueSummary() {
        BigDecimal reservationFees = paymentRepository.sumAmountByStatusAndMethods(
                Payment.PaymentStatus.SUCCESS,
                List.of(Payment.PaymentMethod.CARD, Payment.PaymentMethod.PAYHERE));
        BigDecimal durationCharges = paymentRepository.sumAmountByStatusAndMethods(
                Payment.PaymentStatus.SUCCESS,
                List.of(Payment.PaymentMethod.CASH));

        reservationFees = (reservationFees != null) ? reservationFees : BigDecimal.ZERO;
        durationCharges = (durationCharges != null) ? durationCharges : BigDecimal.ZERO;

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("reservationFees", reservationFees);
        summary.put("durationCharges", durationCharges);
        summary.put("total", reservationFees.add(durationCharges));
        return summary;
    }

    public Map<String, BigDecimal> getRevenueSummaryByHub(Long hubId) {
        BigDecimal reservationFees = paymentRepository.sumAmountByStatusAndMethodsAndHub(
                Payment.PaymentStatus.SUCCESS,
                List.of(Payment.PaymentMethod.CARD, Payment.PaymentMethod.PAYHERE),
                hubId);
        BigDecimal durationCharges = paymentRepository.sumAmountByStatusAndMethodsAndHub(
                Payment.PaymentStatus.SUCCESS,
                List.of(Payment.PaymentMethod.CASH),
                hubId);

        reservationFees = (reservationFees != null) ? reservationFees : BigDecimal.ZERO;
        durationCharges = (durationCharges != null) ? durationCharges : BigDecimal.ZERO;

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("reservationFees", reservationFees);
        summary.put("durationCharges", durationCharges);
        summary.put("total", reservationFees.add(durationCharges));
        return summary;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}