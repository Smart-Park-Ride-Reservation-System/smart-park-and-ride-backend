package com.project.smartparking.service;

import com.project.smartparking.model.Payment;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.repository.PaymentRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        String amount = String.format(java.util.Locale.US, "%.2f",
                r.getReservationFee() != null ? r.getReservationFee() : 200.0);
        String currency = "LKR";
        String orderId = String.valueOf(reservationId);

        String secretHash = md5(merchantSecret).toUpperCase();
        String hash = md5(merchantId + orderId + amount + currency + secretHash).toUpperCase();

        // Append reservationId for frontend redirection polling
        String returnUrlWithId = returnUrl
                + (returnUrl.contains("?") ? "&" : "?")
                + "reservationId=" + reservationId;

        // Dynamically populate user details if available
        String firstName = (r.getUser() != null && r.getUser().getFirstName() != null)
                ? r.getUser().getFirstName()
                : "Customer";
        String lastName = (r.getUser() != null && r.getUser().getLastName() != null)
                ? r.getUser().getLastName()
                : "";
        String email = (r.getUser() != null && r.getUser().getEmail() != null)
                ? r.getUser().getEmail()
                : "noreply@smartparking.lk";
        String phone = (r.getUser() != null && r.getUser().getPhone() != null)
                ? r.getUser().getPhone()
                : "0771234567";

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", merchantId);
        fields.put("return_url", returnUrlWithId);
        fields.put("cancel_url", cancelUrl);
        fields.put("notify_url", notifyUrl);
        fields.put("order_id", orderId);
        fields.put("items",
                "Reservation Fee - Slot #" + (r.getSlot() != null ? r.getSlot().getSlotCode() : reservationId));
        fields.put("currency", currency);
        fields.put("amount", amount);
        fields.put("first_name", firstName);
        fields.put("last_name", lastName);
        fields.put("email", email);
        fields.put("phone", phone);
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

    @Transactional
    public void handleNotify(String merchantId, String orderId, String amount,
            String currency, String statusCode, String md5sig) {

        if (!verifyNotification(merchantId, orderId, amount, currency, statusCode, md5sig)) {
            throw new SecurityException("Invalid PayHere signature");
        }

        Long reservationId = Long.parseLong(orderId);

        Reservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        if ("2".equals(statusCode)) { // Status 2 = PayHere Success

            if (r.getStatus() == Reservation.ReservationStatus.PENDING) {
                r.setReservationFee(new java.math.BigDecimal(amount));
                r.setStatus(Reservation.ReservationStatus.CONFIRMED);
                r.setQrToken(UUID.randomUUID().toString());
                reservationRepo.save(r);

                // Record the reservation fee payment
                Payment payment = new Payment();
                payment.setReservation(r);
                payment.setAmount(BigDecimal.valueOf(Double.parseDouble(amount)));
                payment.setPaymentMethod(Payment.PaymentMethod.PAYHERE);
                payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
                paymentRepo.save(payment);
            }
        } else if ("-3".equals(statusCode)) {
            r.setStatus(Reservation.ReservationStatus.CANCELLED);
            reservationRepo.save(r);
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