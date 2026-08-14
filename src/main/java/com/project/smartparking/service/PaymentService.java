package com.project.smartparking.service;

import com.project.smartparking.model.Payment;
import com.project.smartparking.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final List<String> VALID_STATUSES = List.of("PENDING", "SUCCESS", "FAILED", "REFUNDED");

    private static final List<String> VALID_METHODS = List.of("CARD", "PAYHERE", "CASH", "BANK_TRANSFER");

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }

    public Payment getById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    public List<Payment> getByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }

    public Payment create(Payment payment) {
        if (payment.getReservationId() == null)
            throw new IllegalArgumentException("reservationId is required");

        if (payment.getAmount() == null || payment.getAmount().signum() <= 0)
            throw new IllegalArgumentException("amount must be a positive value");

        if (payment.getPaymentStatus() != null && !VALID_STATUSES.contains(payment.getPaymentStatus()))
            throw new IllegalArgumentException("Invalid payment status: " + payment.getPaymentStatus());

        if (payment.getPaymentMethod() != null && !VALID_METHODS.contains(payment.getPaymentMethod()))
            throw new IllegalArgumentException("Invalid payment method: " + payment.getPaymentMethod());

        return paymentRepository.save(payment);
    }

    public Payment updateStatus(Long id, String status) {
        if (status == null || !VALID_STATUSES.contains(status))
            throw new IllegalArgumentException("Invalid payment status: " + status);

        Payment payment = getById(id);
        payment.setPaymentStatus(status);
        return paymentRepository.save(payment);
    }

    @Transactional
    public synchronized Payment recordPayment(Long reservationId, Double amount, String method) {
        if (reservationId == null)
            throw new IllegalArgumentException("reservationId is required");
        if (amount == null || amount <= 0)
            throw new IllegalArgumentException("amount must be a positive value");
        if (method != null && !VALID_METHODS.contains(method))
            throw new IllegalArgumentException("Invalid payment method: " + method);

        String resolvedMethod = method != null ? method : "CASH";

        List<Payment> existing = paymentRepository.findByReservationId(reservationId);
        Payment duplicate = existing.stream()
                .filter(p -> resolvedMethod.equals(p.getPaymentMethod())
                        && "SUCCESS".equals(p.getPaymentStatus()))
                .findFirst()
                .orElse(null);

        if (duplicate != null) {
            return duplicate;
        }

        Payment payment = new Payment();
        payment.setReservationId(reservationId);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setPaymentMethod(resolvedMethod);
        payment.setPaymentStatus("SUCCESS");

        return paymentRepository.save(payment);
    }

    public Map<String, BigDecimal> getRevenueSummary() {
        List<Payment> successful = paymentRepository.findByPaymentStatus("SUCCESS");

        BigDecimal reservationFees = successful.stream()
                .filter(p -> "CARD".equals(p.getPaymentMethod()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal durationCharges = successful.stream()
                .filter(p -> "CASH".equals(p.getPaymentMethod()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("reservationFees", reservationFees);
        summary.put("durationCharges", durationCharges);
        summary.put("total", reservationFees.add(durationCharges));
        return summary;
    }
}