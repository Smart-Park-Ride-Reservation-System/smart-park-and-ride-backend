package com.project.smartparking.controller;

import com.project.smartparking.service.PayHereService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/payhere")
@CrossOrigin(origins = "http://localhost:3000")
public class PayHereController {

    @Autowired
    private PayHereService payHereService;

    // Called by frontend to get checkout fields + hash
    @GetMapping("/init")
    public ResponseEntity<Map<String, String>> init(@RequestParam Long reservationId) {
        return ResponseEntity.ok(payHereService.buildCheckoutFields(reservationId));
    }

    // Called by PayHere after payment — no JWT, must be public
    @PostMapping("/notify")
    public ResponseEntity<String> notify(
            @RequestParam("merchant_id")      String merchantId,
            @RequestParam("order_id")         String orderId,
            @RequestParam("payhere_amount")   String amount,
            @RequestParam("payhere_currency") String currency,
            @RequestParam("status_code")      String statusCode,
            @RequestParam("md5sig")           String md5sig
    ) {
        try {
            payHereService.handleNotify(merchantId, orderId, amount, currency, statusCode, md5sig);
            return ResponseEntity.ok("OK");
        } catch (SecurityException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}