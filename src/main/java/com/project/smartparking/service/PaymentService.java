package com.project.smartparking.service;

import com.project.smartparking.model.Payment;
import com.project.smartparking.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

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
        return paymentRepository.save(payment);
    }

    public Payment updateStatus(Long id, String status) {
        Payment payment = getById(id);
        payment.setPaymentStatus(status);
        return paymentRepository.save(payment);
    }
}