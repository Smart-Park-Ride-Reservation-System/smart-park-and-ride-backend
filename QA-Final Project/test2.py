"""
SmartPark – Automated UI Test Suite
Covers: REQ-1 (slot display), REQ-5/6/7 (reservation & double-booking),
        REQ-9/10/11 (QR confirmation), REQ-14 (my trips auth),
        REQ-18/19/20 (admin dashboard)

Tool : Selenium WebDriver + pytest (Python)
Run  : pytest test_smartpark.py -v
"""

import time
import pytest
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import NoSuchElementException, TimeoutException

# ─────────────────────────────────────────────
#  CONFIG – update before running
# ─────────────────────────────────────────────
BASE_URL       = "http://localhost:3000"
USER_EMAIL     = "rehenu@gmail.com"
USER_PASSWORD  = "Rehenu"
ADMIN_EMAIL    = "chama.smartpark@gmail.com"
ADMIN_PASSWORD = "Chama"

ARRIVAL_DATE   = "2026-07-01"   # must be a future date
ARRIVAL_TIME   = "08:00"
VEHICLE_NO     = "WP CAT 1234"

WAIT = 10   # seconds for explicit waits


# ─────────────────────────────────────────────
#  FIXTURES
# ─────────────────────────────────────────────
@pytest.fixture(scope="function")
def driver():
    opts = Options()
    opts.add_argument("--headless")
    opts.add_argument("--no-sandbox")
    opts.add_argument("--disable-dev-shm-usage")
    opts.add_argument("--window-size=1280,900")
    d = webdriver.Chrome(options=opts)
    d.implicitly_wait(5)
    yield d
    d.quit()


# ─────────────────────────────────────────────
#  HELPERS
# ─────────────────────────────────────────────
def login(driver, email, password):
    """Log in and wait until redirected away from /login."""
    driver.get(f"{BASE_URL}/login")
    WebDriverWait(driver, WAIT).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, "input[type='email']"))
    )
    driver.find_element(By.CSS_SELECTOR, "input[type='email']").clear()
    driver.find_element(By.CSS_SELECTOR, "input[type='email']").send_keys(email)
    driver.find_element(By.CSS_SELECTOR, "input[type='password']").clear()
    driver.find_element(By.CSS_SELECTOR, "input[type='password']").send_keys(password)
    driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
    WebDriverWait(driver, WAIT).until(EC.url_changes(f"{BASE_URL}/login"))


def search_slots(driver):
    """Navigate to /slots, fill search form and click Find Slots."""
    driver.get(f"{BASE_URL}/slots")
    wait = WebDriverWait(driver, WAIT)

    hub_select = wait.until(
        EC.presence_of_element_located((By.CSS_SELECTOR, "select:nth-of-type(1)"))
    )
    Select(hub_select).select_by_index(1)
    time.sleep(1)   # wait for lots to load

    lot_select = driver.find_element(By.CSS_SELECTOR, "select:nth-of-type(2)")
    Select(lot_select).select_by_index(1)

    driver.find_element(By.CSS_SELECTOR, "input[type='date']").send_keys(ARRIVAL_DATE)
    driver.find_element(By.CSS_SELECTOR, "input[type='time']").send_keys(ARRIVAL_TIME)

    driver.find_element(By.XPATH, "//button[contains(text(),'Find Slots')]").click()

    wait.until(EC.presence_of_element_located(
        (By.XPATH, "//span[contains(text(),'Entrance')]")
    ))


# ─────────────────────────────────────────────
#  1 – Login
# ─────────────────────────────────────────────
class TestLogin:

    def test_user_login_success(self, driver):
        """Valid user credentials redirect away from /login."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        assert "/login" not in driver.current_url

    def test_admin_login_success(self, driver):
        """Valid admin credentials redirect away from /login."""
        login(driver, ADMIN_EMAIL, ADMIN_PASSWORD)
        assert "/login" not in driver.current_url

    def test_invalid_login_stays_on_login(self, driver):
        """Wrong password should not leave /login page."""
        driver.get(f"{BASE_URL}/login")
        WebDriverWait(driver, WAIT).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "input[type='email']"))
        )
        driver.find_element(By.CSS_SELECTOR, "input[type='email']").send_keys(USER_EMAIL)
        driver.find_element(By.CSS_SELECTOR, "input[type='password']").send_keys("wrongpassword")
        driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
        time.sleep(2)
        assert "/login" in driver.current_url


# ─────────────────────────────────────────────
#  2 – Real-Time Slot Display (REQ-1)
# ─────────────────────────────────────────────
class TestSlotDisplay:

    def test_slot_grid_renders(self, driver):
        """REQ-1: Colour-coded slot grid appears after search."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        slots = driver.find_elements(By.XPATH,
            "//div[contains(@class,'rounded-lg') and contains(@class,'shadow-sm')]"
        )
        assert len(slots) > 0, "No slot tiles rendered"

    def test_legend_shows_all_statuses(self, driver):
        """REQ-1: Legend includes Available, Reserved and Occupied."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        page = driver.page_source
        assert "Available" in page
        assert "Reserved" in page
        assert "Occupied" in page

    def test_slot_codes_visible(self, driver):
        """REQ-1: Each slot tile displays its slot code (e.g. A1, B2)."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        codes = driver.find_elements(By.XPATH,
            "//div[contains(@class,'rounded-lg')]//self::div[string-length(text())<=4]"
        )
        assert len(codes) > 0, "Slot codes not visible on tiles"


# ─────────────────────────────────────────────
#  3 – Reservation Flow (REQ-5, REQ-7)
# ─────────────────────────────────────────────
class TestReservation:

    def test_search_without_hub_alerts(self, driver):
        """REQ-5: Clicking Find Slots without a hub raises a browser alert."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        driver.get(f"{BASE_URL}/slots")
        WebDriverWait(driver, WAIT).until(
            EC.presence_of_element_located((By.XPATH, "//button[contains(text(),'Find Slots')]"))
        )
        driver.find_element(By.XPATH, "//button[contains(text(),'Find Slots')]").click()
        alert = WebDriverWait(driver, WAIT).until(EC.alert_is_present())
        assert alert is not None
        alert.accept()

    def test_click_available_slot_shows_summary(self, driver):
        """REQ-5: Clicking an available (green) slot opens the booking summary."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        available = driver.find_elements(By.XPATH,
            "//div[contains(@class,'bg-emerald-500')]"
        )
        if not available:
            pytest.skip("No available slots in test data")
        available[0].click()
        confirm_btn = WebDriverWait(driver, WAIT).until(
            EC.presence_of_element_located((By.XPATH,
                "//button[contains(text(),'Confirm Booking')]"
            ))
        )
        assert confirm_btn.is_displayed()

    def test_confirm_booking_goes_to_details(self, driver):
        """REQ-5: Confirm Booking navigates to /slots/details."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        available = driver.find_elements(By.XPATH,
            "//div[contains(@class,'bg-emerald-500')]"
        )
        if not available:
            pytest.skip("No available slots in test data")
        available[0].click()
        WebDriverWait(driver, WAIT).until(
            EC.element_to_be_clickable((By.XPATH,
                "//button[contains(text(),'Confirm Booking')]"
            ))
        ).click()
        WebDriverWait(driver, WAIT).until(EC.url_contains("/slots/details"))
        assert "/slots/details" in driver.current_url

    def test_empty_vehicle_number_alerts(self, driver):
        """REQ-5: Submitting details without a vehicle number shows an alert."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        available = driver.find_elements(By.XPATH,
            "//div[contains(@class,'bg-emerald-500')]"
        )
        if not available:
            pytest.skip("No available slots in test data")
        available[0].click()
        WebDriverWait(driver, WAIT).until(
            EC.element_to_be_clickable((By.XPATH,
                "//button[contains(text(),'Confirm Booking')]"
            ))
        ).click()
        WebDriverWait(driver, WAIT).until(EC.url_contains("/slots/details"))
        driver.find_element(By.XPATH,
            "//button[contains(text(),'Confirm Reservation')]"
        ).click()
        alert = WebDriverWait(driver, WAIT).until(EC.alert_is_present())
        assert alert is not None
        alert.accept()

    def test_full_reservation_reaches_confirmation(self, driver):
        """REQ-5 + REQ-7: Complete flow lands on /slots/confirmation with QR token."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        available = driver.find_elements(By.XPATH,
            "//div[contains(@class,'bg-emerald-500')]"
        )
        if not available:
            pytest.skip("No available slots in test data")
        available[0].click()
        WebDriverWait(driver, WAIT).until(
            EC.element_to_be_clickable((By.XPATH,
                "//button[contains(text(),'Confirm Booking')]"
            ))
        ).click()
        WebDriverWait(driver, WAIT).until(EC.url_contains("/slots/details"))

        driver.find_element(By.CSS_SELECTOR, "input[type='tel']").send_keys("771234567")
        driver.find_element(By.CSS_SELECTOR, "input[placeholder*='WP']").send_keys(VEHICLE_NO)
        driver.find_element(By.XPATH, "//button[text()='CAR']").click()
        driver.find_element(By.XPATH,
            "//button[contains(text(),'Confirm Reservation')]"
        ).click()

        WebDriverWait(driver, WAIT).until(EC.url_contains("/slots/confirmation"))
        assert "confirmation" in driver.current_url

        page = driver.page_source
        assert "Show to attendant" in page or "Booking Confirmed" in page


# ─────────────────────────────────────────────
#  4 – Double Booking Prevention (REQ-6)
# ─────────────────────────────────────────────
class TestDoubleBooking:

    def test_reserved_slot_not_selectable(self, driver):
        """REQ-6: Reserved (amber) slot cannot be booked."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        reserved = driver.find_elements(By.XPATH,
            "//div[contains(@class,'bg-amber-400')]"
        )
        if not reserved:
            pytest.skip("No reserved slots in test data")
        reserved[0].click()
        time.sleep(1)
        btns = driver.find_elements(By.XPATH,
            "//button[contains(text(),'Confirm Booking')]"
        )
        assert len(btns) == 0, "Reserved slot was selectable – double booking risk!"

    def test_occupied_slot_not_selectable(self, driver):
        """REQ-6: Occupied (red) slot cannot be booked."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        search_slots(driver)
        occupied = driver.find_elements(By.XPATH,
            "//div[contains(@class,'bg-red-500')]"
        )
        if not occupied:
            pytest.skip("No occupied slots in test data")
        occupied[0].click()
        time.sleep(1)
        btns = driver.find_elements(By.XPATH,
            "//button[contains(text(),'Confirm Booking')]"
        )
        assert len(btns) == 0, "Occupied slot was selectable!"


# ─────────────────────────────────────────────
#  5 – QR & Confirmation (REQ-7, REQ-9, REQ-10)
# ─────────────────────────────────────────────
class TestQRConfirmation:

    def test_confirmation_shows_attendant_code(self, driver):
        """REQ-7: Confirmation page shows attendant short-code."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        driver.get(f"{BASE_URL}/slots/confirmation?reservationId=1")
        WebDriverWait(driver, WAIT).until(
            EC.presence_of_element_located((By.XPATH,
                "//span[contains(text(),'Show to attendant')]"
            ))
        )
        assert "Show to attendant" in driver.page_source

    def test_confirmation_shows_booking_fields(self, driver):
        """REQ-7: Confirmation page shows Reservation ID, Slot, Date and Time."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        driver.get(f"{BASE_URL}/slots/confirmation?reservationId=1")
        WebDriverWait(driver, WAIT).until(
            EC.presence_of_element_located((By.XPATH,
                "//span[contains(text(),'Reservation ID')]"
            ))
        )
        page = driver.page_source
        assert "Reservation ID" in page
        assert "Slot"           in page
        assert "Date"           in page
        assert "Time"           in page

    def test_invalid_reservation_id_renders_gracefully(self, driver):
        """REQ-10: Unknown reservationId shows dashes, not a crash."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        driver.get(f"{BASE_URL}/slots/confirmation?reservationId=999999")
        time.sleep(3)
        page = driver.page_source
        assert "Booking Confirmed" in page or "—" in page


# ─────────────────────────────────────────────
#  6 – My Trips (REQ-14)
# ─────────────────────────────────────────────
class TestMyTrips:

    def test_mytrips_accessible_when_logged_in(self, driver):
        """REQ-14: Authenticated user can reach /mytrips."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        driver.get(f"{BASE_URL}/mytrips")
        WebDriverWait(driver, WAIT).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        assert "/mytrips" in driver.current_url

    def test_mytrips_redirects_unauthenticated(self, driver):
        """REQ-14: Unauthenticated request to /mytrips redirects to /login."""
        driver.get(f"{BASE_URL}/mytrips")
        WebDriverWait(driver, WAIT).until(EC.url_contains("/login"))
        assert "/login" in driver.current_url


# ─────────────────────────────────────────────
#  7 – Admin Dashboard (REQ-18, REQ-19, REQ-20)
# ─────────────────────────────────────────────
class TestAdminDashboard:

    def test_admin_can_access_dashboard(self, driver):
        """REQ-18: Admin reaches /admin and sees management content."""
        login(driver, ADMIN_EMAIL, ADMIN_PASSWORD)
        driver.get(f"{BASE_URL}/admin")
        WebDriverWait(driver, WAIT).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        assert "/admin" in driver.current_url
        page = driver.page_source
        assert any(kw in page for kw in ["Dashboard", "Slot", "Reservation", "Admin"])

    def test_admin_slot_management_visible(self, driver):
        """REQ-18: Admin dashboard shows slot/parking management section."""
        login(driver, ADMIN_EMAIL, ADMIN_PASSWORD)
        driver.get(f"{BASE_URL}/admin")
        WebDriverWait(driver, WAIT).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        page = driver.page_source
        assert "Slot" in page or "Parking" in page

    def test_admin_revenue_section_visible(self, driver):
        """REQ-19: Admin dashboard includes a revenue/report section."""
        login(driver, ADMIN_EMAIL, ADMIN_PASSWORD)
        driver.get(f"{BASE_URL}/admin")
        WebDriverWait(driver, WAIT).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        page = driver.page_source
        assert any(kw in page for kw in ["Revenue", "Payment", "Report", "Income"])

    def test_regular_user_blocked_from_admin(self, driver):
        """REQ-18: Regular user cannot access /admin."""
        login(driver, USER_EMAIL, USER_PASSWORD)
        driver.get(f"{BASE_URL}/admin")
        time.sleep(2)
        page = driver.page_source
        redirected = "/admin" not in driver.current_url
        blocked = any(kw in page for kw in
                      ["Unauthorized", "Access Denied", "Forbidden", "403", "login"])
        assert redirected or blocked, "Regular user accessed admin panel!"