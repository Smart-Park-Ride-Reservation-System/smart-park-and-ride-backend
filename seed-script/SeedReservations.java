import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Run directly, no project/build setup needed:
//     java SeedReservations.java
// Requires JDK 17+ (single-file source-code execution, JEP 330 / 458).
public class SeedReservations {

    // Production backend on Railway.
    private static final String API_URL = "https://smartpark-reservation-backend-production.up.railway.app";

    private static final HttpClient client = HttpClient.newHttpClient();

    private static final String HUB_NAME = "Makumbura Multimodal Transport Hub";
    private static final String LOT_NAME = "Train Terminal";

    // How many user+reservation pairs to attempt. Actual count created is
    // capped by however many AVAILABLE slots exist in the target hub/lot,
    // since each reservation locks a slot — see the warning printed at the
    // end of main() if fewer than this many slots were available.
    private static final int TARGET_RESERVATIONS = 50;

    // Reservations get spread across this many upcoming days, at randomized
    // times, instead of every reservation landing on the same date/time.
    private static final int MIN_DAYS_AHEAD = 1;
    private static final int MAX_DAYS_AHEAD = 14;

    // RFC 2606 reserves the ".invalid" TLD specifically for addresses that
    // are guaranteed to never resolve or exist — safer than ".com" for a
    // notification system that might actually try to send email, since it
    // can't accidentally reach a real inbox.
    private static final String EMAIL_DOMAIN = "example.invalid";

    // ── random data pools ───────────────────────────────────────────────────
    private static final Random RNG = new Random();

    private static final String[] FIRST_NAMES = {
        "Nimal", "Sunil", "Kasun", "Saman", "Ruwan", "Chamara", "Dilshan", "Nuwan",
        "Tharindu", "Pradeep", "Lakmal", "Asanka", "Chathura", "Janaka", "Ravindu",
        "Nadeesha", "Kumari", "Sachini", "Dilani", "Hashini", "Anusha", "Tharushi",
        "Madhavi", "Pavithra", "Iresha", "Vinodhya", "Shanika", "Nethmi", "Yasodha",
        "Roshan", "Ajith", "Mahinda", "Sampath", "Gayan", "Isuru", "Buddhika",
        "Kalana", "Ishan", "Malith", "Chanaka", "Supun", "Danushka", "Charith",
        "Amila", "Sameera", "Kavindu", "Damith", "Harsha",
        "Piumi", "Sanduni", "Chathurika", "Dulani", "Nimesha", "Rashmi", "Thilini",
        "Oshadi", "Gimhani", "Sewwandi", "Kaveesha", "Umesha"
    };

    private static final String[] LAST_NAMES = {
        "Perera", "Fernando", "Silva", "Jayawardena", "Wickramasinghe", "Bandara",
        "Rajapaksa", "Gunasekara", "Dissanayake", "Senanayake", "Wijesinghe",
        "Karunaratne", "Abeysekara", "Rathnayake", "Weerasinghe", "Amarasinghe",
        "Gunawardena", "Hettiarachchi", "Madushanka", "Pathirana", "Ekanayake",
        "Wickramaratne", "Liyanage", "Kodithuwakku", "Herath", "Jayasuriya",
        "Ranasinghe", "Mendis", "Peiris", "Dias"
    };

    // Sri Lankan vehicle registration province/district prefixes
    private static final String[] PLATE_PREFIXES = {
        "WP", "CP", "SP", "NP", "EP", "NWP", "NCP", "UP", "SGP"
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

    // firstname.lastnameN@example.invalid — reads like a normal test email
    // instead of a name jammed against a millisecond timestamp. N is the
    // 1-based slot index for this run, so it's small and unique per account
    // created in a single run without needing a giant suffix. Re-running
    // the script may collide with a previous run's emails; that's fine —
    // signup failures are caught per-slot in main() and just get skipped.
    private static String makeEmail(String firstName, String lastName, int n) {
        return (firstName + "." + lastName + n).toLowerCase(Locale.ROOT) + "@" + EMAIL_DOMAIN;
    }

    // Picks a random day between MIN_DAYS_AHEAD and MAX_DAYS_AHEAD from now,
    // and a random time on the half hour between 06:00 and 22:00, so
    // reservations are spread out instead of all landing on one slot.
    private static String randomScheduledIn() {
        int daysAhead = MIN_DAYS_AHEAD + RNG.nextInt(MAX_DAYS_AHEAD - MIN_DAYS_AHEAD + 1);
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        int hour = 6 + RNG.nextInt(17); // 06:00–22:00
        int minute = RNG.nextBoolean() ? 0 : 30;
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return String.format("%sT%02d:%02d:00", dateStr, hour, minute);
    }

    public static void main(String[] args) throws Exception {
        Long hubId = findHubIdNoAuth(HUB_NAME);
        Long lotId = findLotIdNoAuth(hubId, LOT_NAME);

        // /api/slots/search requires an authenticated COMMUTER or ADMIN, so
        // create a throwaway seed account up front and log in once, purely to
        // get a token for searching slots. Small random suffix so re-running
        // the script doesn't collide with a previous run's seed account.
        String seedEmail = ("seed.scanner" + (10 + RNG.nextInt(90)) + "@" + EMAIL_DOMAIN).toLowerCase(Locale.ROOT);
        String seedPassword = "Test@1234";
        signup("Seed", "Scanner", seedEmail, seedPassword);
        String[] seedAuth = login(seedEmail, seedPassword);
        String seedToken = seedAuth[0];

        List<Long> availableSlots = findAvailableSlots(seedToken, hubId, lotId);

        int toCreate = Math.min(TARGET_RESERVATIONS, availableSlots.size());
        if (availableSlots.size() < TARGET_RESERVATIONS) {
            System.out.println("WARNING: only " + availableSlots.size() + " AVAILABLE slot(s) found in \""
                    + LOT_NAME + "\" — requested " + TARGET_RESERVATIONS
                    + ", will create " + toCreate + " instead.");
        }

        int count = 0;
        for (int i = 0; i < toCreate; i++) {
            Long slotId = availableSlots.get(i);
            try {
                // 1. create a brand-new account for this slot, with a random Sri Lankan name
                String firstName = randomFrom(FIRST_NAMES);
                String lastName  = randomFrom(LAST_NAMES);
                String email = makeEmail(firstName, lastName, i + 1);
                String password = "Test@1234";

                signup(firstName, lastName, email, password);

                // 2. log in as that new account
                String[] auth = login(email, password); // [token, userId]
                String token  = auth[0];
                Long userId   = Long.parseLong(auth[1]);

                // 3. create a reservation for this slot from this account, on a
                //    randomized upcoming date/time — mirrors the booking-details
                //    page's POST /api/reservations payload exactly.
                String scheduledIn = randomScheduledIn();
                String phone = randomPhone();
                String vehicleNo = randomVehicleNo();
                Long reservationId = createReservation(token, userId, slotId, vehicleNo, scheduledIn, phone);

                // 4. confirm payment — mirrors the payment-gateway page's
                //    PUT /api/reservations/{id}/confirm-payment call. The
                //    gateway's card fields (number/name/expiry/CVV) are
                //    validated client-side only and never sent to the
                //    backend, so there's nothing to fabricate here beyond
                //    the confirm call itself.
                confirmPayment(token, reservationId);

                count++;
                System.out.println("Account " + email + " reserved slotId=" + slotId
                        + " reservationId=" + reservationId + " scheduledIn=" + scheduledIn
                        + " phone=" + phone + " — payment confirmed");
            } catch (Exception e) {
                System.out.println("Failed slotId=" + slotId + ": " + e.getMessage());
            }
        }
        System.out.println("Done. Total reservations created & paid: " + count + " / " + TARGET_RESERVATIONS + " requested.");
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
        if (res.statusCode() != 200 && res.statusCode() != 201)
            throw new RuntimeException("Signup failed (HTTP " + res.statusCode() + "): " + res.body());
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
            throw new RuntimeException("Login failed (HTTP " + res.statusCode() + "): " + res.body());

        Object root = Json.parse(res.body());
        Object tokenVal  = field(root, "token");
        Object userIdVal = field(root, "userId");
        if (tokenVal == null || userIdVal == null)
            throw new RuntimeException("Login response missing token/userId: " + res.body());

        return new String[]{ tokenVal.toString(), String.valueOf(asLong(userIdVal)) };
    }

    // ── lookups (no auth needed, adjust to your real endpoints) ────────────────
    private static Long findHubIdNoAuth(String name) throws Exception {
        String body = getNoAuth("/api/hubs");
        Object root = Json.parse(body);
        for (Object hub : iterableOf(root)) {
            Object nameVal = field(hub, "name");
            Object idVal   = field(hub, "hubId");
            if (nameVal != null && idVal != null && name.equals(nameVal)) {
                return asLong(idVal);
            }
        }
        throw new RuntimeException("Hub not found: " + name);
    }

    private static Long findLotIdNoAuth(Long hubId, String lotName) throws Exception {
        String body = getNoAuth("/api/lots/by-hub/" + hubId);
        Object root = Json.parse(body);
        for (Object lot : iterableOf(root)) {
            Object nameVal = field(lot, "name");
            Object idVal   = field(lot, "lotId");
            if (nameVal != null && idVal != null && lotName.equals(nameVal)) {
                return asLong(idVal);
            }
        }
        throw new RuntimeException("Lot not found: " + lotName);
    }

    private static List<Long> findAvailableSlots(String token, Long hubId, Long lotId) throws Exception {
        String body = getWithAuth("/api/slots/search?hubId=" + hubId + "&lotId=" + lotId, token);
        Object root = Json.parse(body);
        List<Long> result = new ArrayList<>();
        for (Object slot : iterableOf(root)) {
            Object statusVal = field(slot, "status");
            Object idVal     = field(slot, "slotId");
            String status = statusVal == null ? null : statusVal.toString();
            if (idVal != null && "AVAILABLE".equals(status)) {
                result.add(asLong(idVal));
            }
        }
        return result;
    }

    // Handles both a bare JSON array response and a wrapped object like
    // {"content":[...]} or {"data":[...]} (common with paginated endpoints).
    @SuppressWarnings("unchecked")
    private static List<Object> iterableOf(Object node) {
        if (node instanceof List) return (List<Object>) node;
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            if (map.get("content") instanceof List) return (List<Object>) map.get("content");
            if (map.get("data") instanceof List) return (List<Object>) map.get("data");
        }
        throw new RuntimeException("Expected a JSON array but got: " + node);
    }

    @SuppressWarnings("unchecked")
    private static Object field(Object obj, String key) {
        if (obj instanceof Map) return ((Map<String, Object>) obj).get(key);
        return null;
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Double) return ((Double) o).longValue();
        if (o instanceof String) return Long.parseLong((String) o);
        throw new RuntimeException("Expected a numeric value but got: " + o);
    }

    // ── reservation creation ────────────────────────────────────────────────
    // Mirrors the booking-details page's POST /api/reservations payload
    // exactly (slotId, userId, vehicleNo, vehicleType, scheduledIn, phone).
    // Returns the new reservationId, parsed from the response body, since
    // confirmPayment() needs it next.
    private static Long createReservation(String token, Long userId, long slotId,
                                           String vehicleNo, String scheduledIn, String phone) throws Exception {
        String body = String.format(
            "{\"slotId\":%d,\"userId\":%d,\"vehicleNo\":\"%s\",\"vehicleType\":\"CAR\"," +
            "\"scheduledIn\":\"%s\",\"phone\":\"%s\"}",
            slotId, userId, vehicleNo, scheduledIn, phone
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/reservations"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200 && res.statusCode() != 201)
            throw new RuntimeException("Create reservation HTTP " + res.statusCode() + ": " + res.body());

        Object root = Json.parse(res.body());
        Object idVal = field(root, "reservationId");
        if (idVal == null)
            throw new RuntimeException("Reservation response missing reservationId: " + res.body());
        return asLong(idVal);
    }

    // ── payment confirmation ────────────────────────────────────────────────
    // Mirrors the payment-gateway page's PUT /api/reservations/{id}/confirm-payment
    // call. No body — the gateway UI collects card details purely for
    // client-side validation and never forwards them to the backend.
    private static void confirmPayment(String token, Long reservationId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/api/reservations/" + reservationId + "/confirm-payment"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200 && res.statusCode() != 201)
            throw new RuntimeException("Confirm payment HTTP " + res.statusCode() + ": " + res.body());
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────
    private static String getNoAuth(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + path))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new RuntimeException(
                "GET " + path + " failed with HTTP " + res.statusCode() +
                " — check the backend is running and the endpoint exists. Body: " + res.body()
            );
        }
        return res.body();
    }

    private static String getWithAuth(String path, String token) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new RuntimeException(
                "GET " + path + " failed with HTTP " + res.statusCode() +
                " — check the token is valid and the account has the required role. Body: " + res.body()
            );
        }
        return res.body();
    }

    // ── minimal JSON parser (replaces Jackson so this file has zero external
    //    dependencies and can be run with `java SeedReservations.java`) ──────
    // Produces: Map<String,Object> for objects, List<Object> for arrays,
    // String, Long/Double, Boolean, or null for scalars.
    private static final class Json {
        private final String s;
        private int i = 0;

        private Json(String s) { this.s = s; }

        static Object parse(String s) {
            Json p = new Json(s);
            p.skipWs();
            return p.parseValue();
        }

        private void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private Object parseValue() {
            skipWs();
            char c = s.charAt(i);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': i += 4; return Boolean.TRUE;
                case 'f': i += 5; return Boolean.FALSE;
                case 'n': i += 4; return null;
                default:  return parseNumber();
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // consume '{'
            skipWs();
            if (s.charAt(i) == '}') { i++; return map; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                i++; // consume ':'
                Object value = parseValue();
                map.put(key, value);
                skipWs();
                char ch = s.charAt(i);
                if (ch == ',') { i++; continue; }
                if (ch == '}') { i++; break; }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            i++; // consume '['
            skipWs();
            if (s.charAt(i) == ']') { i++; return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                char ch = s.charAt(i);
                if (ch == ',') { i++; skipWs(); continue; }
                if (ch == ']') { i++; break; }
            }
            return list;
        }

        private String parseString() {
            i++; // consume opening quote
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char esc = s.charAt(i++);
                    switch (esc) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'n':  sb.append('\n'); break;
                        case 't':  sb.append('\t'); break;
                        case 'r':  sb.append('\r'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default: sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object parseNumber() {
            int start = i;
            while (i < s.length() && "-+.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
            String num = s.substring(start, i);
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                return Double.parseDouble(num);
            }
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                return Double.parseDouble(num);
            }
        }
    }
}