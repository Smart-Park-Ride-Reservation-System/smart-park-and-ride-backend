package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class SeedReservations {

    private static final String API_URL = "http://localhost:8080"; // adjust to your backend port
    private static final HttpClient client = HttpClient.newHttpClient();

    private static final String HUB_NAME = "Makumbura Multimodal Transport Hub";
    private static final String LOT_NAME = "Train Terminal";
    private static final String DATE     = LocalDate.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    private static final String TIME     = "22:05";

    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    // ── random data pools ───────────────────────────────────────────────────
    private static final Random RNG = new Random();

    private static final String[] FIRST_NAMES = {
        "Nimal", "Sunil", "Kasun", "Saman", "Ruwan", "Chamara", "Dilshan", "Nuwan",
        "Tharindu", "Pradeep", "Lakmal", "Asanka", "Chathura", "Janaka", "Ravindu",
        "Nadeesha", "Kumari", "Sachini", "Dilani", "Hashini", "Anusha", "Tharushi",
        "Madhavi", "Pavithra", "Iresha", "Vinodhya", "Shanika", "Nethmi", "Yasodha",
        "Roshan", "Ajith", "Mahinda", "Sampath", "Gayan", "Isuru", "Buddhika"
    };

    private static final String[] LAST_NAMES = {
        "Perera", "Fernando", "Silva", "Jayawardena", "Wickramasinghe", "Bandara",
        "Rajapaksa", "Gunasekara", "Dissanayake", "Senanayake", "Wijesinghe",
        "Karunaratne", "Abeysekara", "Rathnayake", "Weerasinghe", "Amarasinghe",
        "Gunawardena", "Hettiarachchi", "Madushanka", "Pathirana", "Ekanayake",
        "Wickramaratne", "Liyanage", "Kodithuwakku"
    };

    // Sri Lankan vehicle registration province/district prefixes
    private static final String[] PLATE_PREFIXES = {
        "WP", // Western
        "CP", // Central
        "SP", // Southern
        "NP", // Northern
        "EP", // Eastern
        "NWP", // North Western
        "NCP", // North Central
        "UP", // Uva
        "SGP" // Sabaragamuwa
    };

    private static String randomFrom(String[] arr) {
        return arr[RNG.nextInt(arr.length)];
    }

    private static String randomVehicleNo() {
        String prefix = randomFrom(PLATE_PREFIXES);
        StringBuilder letterPart = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            letterPart.append((char) ('A' + RNG.nextInt(26)));
        }
        int number = 1000 + RNG.nextInt(9000); // 4-digit number
        return prefix + "-" + letterPart + "-" + number;
    }

    private static String randomPhone() {
        // Sri Lankan mobile prefixes: 070-078 are common
        int prefix = 70 + RNG.nextInt(9);
        int rest = 1000000 + RNG.nextInt(9000000);
        return "+947" + (prefix % 10) + rest;
    }

    public static void main(String[] args) throws Exception {
        Long hubId = findHubIdNoAuth(HUB_NAME);
        Long lotId = findLotIdNoAuth(hubId, LOT_NAME);
        List<Long> availableSlots = findAvailableSlotsNoAuth(lotId);

        int count = 0;
        for (Long slotId : availableSlots) {
            try {
                // 1. create a brand-new account for this slot, with a random Sri Lankan name
                String firstName = randomFrom(FIRST_NAMES);
                String lastName  = randomFrom(LAST_NAMES);
                String email = (firstName + "." + lastName + RUN_ID + "_" + count + "@example.com")
                        .toLowerCase(Locale.ROOT);
                String password = "Test@1234";

                signup(firstName, lastName, email, password);

                // 2. log in as that new account
                String[] auth = login(email, password); // [token, userId]
                String token  = auth[0];
                Long userId   = Long.parseLong(auth[1]);

                // 3. make a reservation for this slot from this account
                createReservation(token, userId, slotId, count);

                count++;
                System.out.println("Account " + email + " reserved slotId=" + slotId + " with fee 200");
            } catch (Exception e) {
                System.out.println("Failed slotId=" + slotId + ": " + e.getMessage());
            }
        }
        System.out.println("Done. Total reservations created: " + count);
    }

    // ── signup ───────────────────────────────────────────────────────────────
    private static void signup(String firstName, String lastName, String email, String password) throws Exception {
        String body = String.format(
            "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
            firstName, lastName, email, password
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/auth/signup"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new RuntimeException("Signup failed: " + res.body());
    }

    // ── login ────────────────────────────────────────────────────────────────
    private static String[] login(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new RuntimeException("Login failed: " + res.body());

        String token  = extract(res.body(), "\"token\"\\s*:\\s*\"([^\"]+)\"");
        String userId = extract(res.body(), "\"userId\"\\s*:\\s*(\\d+)");
        return new String[]{ token, userId };
    }

    // ── lookups (no auth needed, adjust to your real endpoints) ────────────────
    private static Long findHubIdNoAuth(String name) throws Exception {
        String body = getNoAuth("/api/hubs");
        Matcher m = Pattern.compile(
            "\\{[^}]*\"hubId\"\\s*:\\s*(\\d+)[^}]*\"name\"\\s*:\\s*\"" + Pattern.quote(name) + "\"[^}]*\\}"
        ).matcher(body);
        if (m.find()) return Long.parseLong(m.group(1));
        throw new RuntimeException("Hub not found: " + name);
    }

    private static Long findLotIdNoAuth(Long hubId, String lotName) throws Exception {
        String body = getNoAuth("/api/lots/hub/" + hubId);
        Matcher m = Pattern.compile(
            "\\{[^}]*\"lotId\"\\s*:\\s*(\\d+)[^}]*\"name\"\\s*:\\s*\"" + Pattern.quote(lotName) + "\"[^}]*\\}"
        ).matcher(body);
        if (m.find()) return Long.parseLong(m.group(1));
        throw new RuntimeException("Lot not found: " + lotName);
    }

    private static List<Long> findAvailableSlotsNoAuth(Long lotId) throws Exception {
        String body = getNoAuth("/api/slots/lot/" + lotId + "?date=" + DATE + "&time=" + TIME);
        List<Long> result = new ArrayList<>();
        Matcher m = Pattern.compile(
            "\\{[^}]*\"slotId\"\\s*:\\s*(\\d+)[^}]*\"status\"\\s*:\\s*\"AVAILABLE\"[^}]*\\}"
        ).matcher(body);
        while (m.find()) result.add(Long.parseLong(m.group(1)));
        return result;
    }

    // ── reservation creation ────────────────────────────────────────────────
    private static void createReservation(String token, Long userId, long slotId, int index) throws Exception {
        String vehicleNo = randomVehicleNo();
        String phone     = randomPhone();

        String body = String.format(
            "{\"slotId\":%d,\"userId\":%d,\"vehicleNo\":\"%s\",\"vehicleType\":\"CAR\"," +
            "\"scheduledIn\":\"%sT%s:00\",\"phone\":\"%s\"}",
            slotId, userId, vehicleNo, DATE, TIME, phone
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/reservations"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new RuntimeException(res.body());
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private static String getNoAuth(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String extract(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) return m.group(1);
        throw new RuntimeException("Field not found in: " + json);
    }
}