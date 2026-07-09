package com.project.smartparking.service;

import com.project.smartparking.model.Payment;
import com.project.smartparking.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    private static final List<String> VALID_STATUSES =
            List.of("PENDING", "SUCCESS", "FAILED", "REFUNDED");

    private static final List<String> VALID_METHODS =
            List.of("CARD", "PAYHERE", "CASH", "BANK_TRANSFER");

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

        // Let @PrePersist in Payment handle default status ("PENDING") if not set
        return paymentRepository.save(payment);
    }

    public Payment updateStatus(Long id, String status) {
        if (status == null || !VALID_STATUSES.contains(status))
            throw new IllegalArgumentException("Invalid payment status: " + status);

        Payment payment = getById(id);
        payment.setPaymentStatus(status);
        return paymentRepository.save(payment);
    }

    /**
     * Records a completed parking-charge payment for a reservation.
     * Called by ReservationService.markPaid() — this is the ONLY place
     * that should ever insert a "parking charge paid" row into `payments`.
     *
     * amount here is the checkout charge only (hours * rate).
     * The 200 Rs reservationFee is intentionally never passed through here —
     * it lives solely on the reservation row and is summed separately
     * on the frontend (totalFeeRevenue + totalPaymentRevenue).
     */
    public Payment recordPayment(Long reservationId, Double amount, String method) {
        if (reservationId == null)
            throw new IllegalArgumentException("reservationId is required");
        if (amount == null || amount <= 0)
            throw new IllegalArgumentException("amount must be a positive value");
        if (method != null && !VALID_METHODS.contains(method))
            throw new IllegalArgumentException("Invalid payment method: " + method);

        Payment payment = new Payment();
        payment.setReservationId(reservationId);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setPaymentMethod(method != null ? method : "CASH");
        payment.setPaymentStatus("SUCCESS");
        // paymentDate is set automatically by Payment's @PrePersist

        return paymentRepository.save(payment);
    }
}