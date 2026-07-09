package com.project.smartparking.controller;

import com.project.smartparking.model.Reservation;
import com.project.smartparking.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gate")
public class GateController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping(value = "/checkin", produces = MediaType.TEXT_HTML_VALUE)
    public String checkin(@RequestParam("token") String token) {
        try {
            Reservation r = reservationService.checkInByQrToken(token);
            return renderResult(true, "Access Granted",
                    "Slot #" + r.getSlotId() + " · Vehicle: " + r.getVehicleNo());
        } catch (IllegalArgumentException e) {
            return renderResult(false, "Invalid QR Code", e.getMessage());
        } catch (IllegalStateException e) {
            return renderResult(false, "Access Denied", e.getMessage());
        }
    }

    private String renderResult(boolean granted, String title, String detail) {
        String color = granted ? "#16a34a" : "#dc2626";
        String icon = granted ? "&#10003;" : "&#10007;";
        return "<html><body style='font-family:Arial,sans-serif;text-align:center;padding:60px;'>"
                + "<div style='font-size:64px;color:" + color + ";'>" + icon + "</div>"
                + "<h1 style='color:" + color + ";'>" + title + "</h1>"
                + "<p style='font-size:18px;color:#333;'>" + detail + "</p>"
                + "</body></html>";
    }
}