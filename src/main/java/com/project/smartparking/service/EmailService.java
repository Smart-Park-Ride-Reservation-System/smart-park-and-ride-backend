package com.project.smartparking.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Original simple text email method (kept in case other parts of the app use it)
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    // New method: HTML booking confirmation with embedded QR code
    public void sendBookingConfirmation(String to, String slotId, String date, String time, String qrData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Booking Confirmed - Slot " + slotId);

            String encodedData = URLEncoder.encode(qrData, StandardCharsets.UTF_8);
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedData;

            String htmlBody = "<div style='font-family:Arial,sans-serif;'>"
                    + "<h2>Your Parking Slot is Confirmed!</h2>"
                    + "<p><b>Slot ID:</b> " + slotId + "</p>"
                    + "<p><b>Date:</b> " + date + "</p>"
                    + "<p><b>Time:</b> " + time + "</p>"
                    + "<p>Show this QR code at the entrance:</p>"
                    + "<img src='" + qrUrl + "' width='200' height='200' />"
                    + "</div>";

            helper.setText(htmlBody, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}