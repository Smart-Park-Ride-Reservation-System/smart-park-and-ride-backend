package com.project.smartparking;

import com.project.smartparking.dto.LoginRequest;
import com.project.smartparking.dto.SignupRequest;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.repository.ParkingSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FullReservationFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    private String jwtToken;
    private Long createdUserId;
    private Long createdReservationId;
    private String qrToken;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: User Signup & Token Extraction")
    void testUserSignupAndLogin() {
        System.out.println("\n========================================================");
        System.out.println(" RUNNING TEST CASE 1: USER SIGNUP & LOGIN");
        System.out.println("========================================================");

        String testEmail = "testuser_" + System.currentTimeMillis() + "@example.com";
        String testPassword = "Password123!";

        SignupRequest signup = new SignupRequest();
        signup.setEmail(testEmail);
        signup.setPassword(testPassword);
        signup.setFirstName("Test");
        signup.setLastName("User");
        signup.setPhone("077" + (System.currentTimeMillis() % 10000000L));

        ResponseEntity<Map> signupResp = restTemplate.postForEntity(getBaseUrl() + "/api/auth/signup", signup, Map.class);
        assertEquals(HttpStatus.CREATED, signupResp.getStatusCode(), "Signup should return HTTP 201 CREATED");
        assertNotNull(signupResp.getBody());

        // Perform Login to get JWT Token
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(testEmail);
        loginReq.setPassword(testPassword);

        ResponseEntity<Map> loginResp = restTemplate.postForEntity(getBaseUrl() + "/api/auth/login", loginReq, Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode(), "Login should return HTTP 200 OK");
        assertNotNull(loginResp.getBody());

        jwtToken = (String) loginResp.getBody().get("token");
        Number uId = (Number) loginResp.getBody().get("userId");
        if (uId != null) createdUserId = uId.longValue();

        assertNotNull(jwtToken, "JWT Token should not be null after login");
        System.out.println(" [PASS] User Signup & Login successful. JWT Token obtained.");
        System.out.println("  User Email: " + testEmail);
        System.out.println("  User ID: " + createdUserId);
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Health Check & System Verification")
    void testHealthCheck() {
        System.out.println("\n========================================================");
        System.out.println(" RUNNING TEST CASE 2: SYSTEM HEALTH CHECK");
        System.out.println("========================================================");

        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) headers.setBearerAuth(jwtToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(getBaseUrl() + "/", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Health check should return HTTP 200 OK");
        System.out.println(" [PASS] System health endpoint returned success: " + response.getBody());
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Create User Reservation & Confirm Payment for QR Code Token")
    void testCreateReservation() {
        System.out.println("\n========================================================");
        System.out.println(" RUNNING TEST CASE 3: CREATE USER RESERVATION & CONFIRM PAYMENT");
        System.out.println("========================================================");

        assertNotNull(jwtToken, "JWT Token must exist from signup");
        assertNotNull(createdUserId, "User ID must exist");

        // Find an available parking slot dynamically from database
        ParkingSlot targetSlot = parkingSlotRepository.findAll().stream()
                .filter(s -> s.getStatus() == ParkingSlot.SlotStatus.AVAILABLE)
                .findFirst()
                .orElseGet(() -> parkingSlotRepository.findAll().get(0));

        Long targetSlotId = targetSlot.getSlotId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        LocalDateTime futureStart = LocalDateTime.now().plusDays(1).plusHours(2);
        LocalDateTime futureEnd = futureStart.plusHours(2);

        Map<String, Object> reservationReq = Map.of(
                "user", Map.of("userId", createdUserId),
                "slot", Map.of("slotId", targetSlotId),
                "vehicleNo", "CAB-" + (System.currentTimeMillis() % 10000L),
                "vehicleType", "CAR",
                "scheduledIn", futureStart.toString(),
                "scheduledOut", futureEnd.toString()
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(reservationReq, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(getBaseUrl() + "/api/reservations", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Reservation creation should return HTTP 200");
        assertNotNull(response.getBody());

        Number resId = (Number) response.getBody().get("reservationId");
        if (resId != null) createdReservationId = resId.longValue();
        assertNotNull(createdReservationId, "Reservation ID must not be null");

        // Confirm payment to generate QR Token & transition to CONFIRMED status
        String confirmUrl = getBaseUrl() + "/api/reservations/" + createdReservationId + "/confirm-payment";
        ResponseEntity<Map> confirmResp = restTemplate.exchange(confirmUrl, HttpMethod.PUT, request, Map.class);
        assertEquals(HttpStatus.OK, confirmResp.getStatusCode(), "Confirm payment should return HTTP 200");
        assertNotNull(confirmResp.getBody());

        qrToken = (String) confirmResp.getBody().get("qrToken");
        assertNotNull(qrToken, "QR Code Token must be generated after payment confirmation");

        System.out.println(" [PASS] Reservation Created & Payment Confirmed.");
        System.out.println("  Reservation ID: " + createdReservationId);
        System.out.println("  Target Slot ID: " + targetSlotId);
        System.out.println("  Generated QR Token: " + qrToken);
        System.out.println("  Reservation Status: " + confirmResp.getBody().get("status"));
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: QR Code Check-In Gate Scan")
    void testQrCheckIn() {
        System.out.println("\n========================================================");
        System.out.println(" RUNNING TEST CASE 4: GATE QR CODE CHECK-IN SCAN");
        System.out.println("========================================================");

        assertNotNull(qrToken, "QR token must exist from previous step");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String scanUrl = getBaseUrl() + "/gate/checkin?token=" + qrToken;
        ResponseEntity<String> response = restTemplate.exchange(scanUrl, HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Gate check-in scan should return HTTP 200 OK");
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Access Granted"), "Gate check-in scan should output Access Granted");

        System.out.println(" [PASS] QR Code Check-In Scan Verified Successfully.");
        System.out.println("  Gate Output: Access Granted");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: QR Code Check-Out Gate Scan")
    void testQrCheckOut() {
        System.out.println("\n========================================================");
        System.out.println(" RUNNING TEST CASE 5: GATE QR CODE CHECK-OUT SCAN");
        System.out.println("========================================================");

        assertNotNull(qrToken, "QR token must exist from previous step");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String scanUrl = getBaseUrl() + "/api/reservations/gate/scan?qrToken=" + qrToken;
        ResponseEntity<Map> response = restTemplate.exchange(scanUrl, HttpMethod.PUT, request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "QR scan should return HTTP 200 OK");
        assertNotNull(response.getBody());

        Boolean ok = (Boolean) response.getBody().get("ok");
        assertTrue(ok != null && ok, "Gate scan check-out should indicate success");

        System.out.println(" [PASS] QR Code Check-Out Scan Verified Successfully.");
        System.out.println("  Gate Scan Message: " + response.getBody().get("message"));
        System.out.println("========================================================\n");
    }
}
