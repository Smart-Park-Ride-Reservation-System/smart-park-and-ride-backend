package com.project.smartparking.service;

import com.project.smartparking.model.Payment;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.PaymentRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PayHereService {

    @Value("${payhere.merchant.id}")
    private String merchantId;

    @Value("${payhere.merchant.secret}")
    private String merchantSecret;

    @Value("${payhere.return.url}")
    private String returnUrl;

    @Value("${payhere.cancel.url}")
    private String cancelUrl;

    @Value("${payhere.notify.url}")
    private String notifyUrl;

    @Autowired
    private ReservationRepository reservationRepo;

    @Autowired
    private PaymentRepository paymentRepo;

    public Map<String, String> buildCheckoutFields(Long reservationId) {
        Reservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        String amount = String.format("%.2f", r.getReservationFee() != null ? r.getReservationFee() : 200.0);
        String currency = "LKR";
        String orderId = String.valueOf(reservationId);

        String secretHash = md5(merchantSecret).toUpperCase();
        String hash = md5(merchantId + orderId + amount + currency + secretHash).toUpperCase();

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", merchantId);
        fields.put("return_url", returnUrl);
        fields.put("cancel_url", cancelUrl);
        fields.put("notify_url", notifyUrl);
        fields.put("order_id", orderId);
        fields.put("items", "Parking Slot Reservation #" + reservationId);
        fields.put("currency", currency);
        fields.put("amount", amount);
        fields.put("first_name", "Customer");
        fields.put("last_name", "");
        fields.put("email", "noreply@smartparking.lk");
        fields.put("phone", "0771234567");
        fields.put("address", "Smart Parking Hub");
        fields.put("city", "Colombo");
        fields.put("country", "Sri Lanka");
        fields.put("hash", hash);
        return fields;
    }

    public boolean verifyNotification(String merchantId, String orderId, String amount,
            String currency, String statusCode, String md5sig) {
        String secretHash = md5(merchantSecret).toUpperCase();
        String expected = md5(merchantId + orderId + amount + currency + statusCode + secretHash).toUpperCase();
        return expected.equals(md5sig.toUpperCase());
    }

    public void handleNotify(String merchantId, String orderId, String amount,
            String currency, String statusCode, String md5sig) {

        if (!verifyNotification(merchantId, orderId, amount, currency, statusCode, md5sig)) {
            throw new SecurityException("Invalid PayHere signature");
        }

        Long reservationId = Long.parseLong(orderId);

        String paymentStatus;
        if ("2".equals(statusCode)) {
            paymentStatus = "SUCCESS";
        } else if ("-3".equals(statusCode)) {
            paymentStatus = "REFUNDED";
        } else {
            paymentStatus = "FAILED";
        }

        Payment payment = new Payment();
        payment.setReservationId(reservationId);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentStatus(paymentStatus);
        payment.setPaymentMethod("PAYHERE");
        paymentRepo.save(payment);

        if ("SUCCESS".equals(paymentStatus)) {
            Reservation r = reservationRepo.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Reservation not found"));
            if ("PENDING".equals(r.getStatus())) {
                r.setStatus("CONFIRMED");
                r.setQrToken(UUID.randomUUID().toString());
                reservationRepo.save(r);
            }
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 failed", e);
        }
    }
}