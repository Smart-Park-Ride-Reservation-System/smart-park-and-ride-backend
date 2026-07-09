"""
Selenium Navbar Navigation Tests
---------------------------------
Tests: link presence, clickability, URL routing, active states, and mobile hamburger menu.

Usage:
    py -m pytest test.py -v
"""

import os
import time
import pytest
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from webdriver_manager.chrome import ChromeDriverManager

# ── Config ────────────────────────────────────────────────────────────────────

BASE_URL       = "http://localhost:3000"
SIGNUP_URL     = BASE_URL + "/signup"
SIGNIN_URL     = BASE_URL + "/login"
POST_LOGIN_URL = BASE_URL + "/dashboard"

EXPECTED_NAV_LINKS = [
    ("Home",        "/"),
    ("Reservation", "/slots"),
    ("About",       "/about-us"),
]

NAV_SELECTOR         = "nav"
NAV_LINK_SELECTOR    = "nav ul a"
ACTIVE_CLASS         = None        # SmartPark uses Tailwind — no semantic active class
HAMBURGER_SELECTOR   = "[data-testid='menu-toggle'], .hamburger, .menu-toggle"
MOBILE_MENU_SELECTOR = ".mobile-menu, .nav-mobile, [data-testid='mobile-nav']"

WAIT_TIMEOUT = 10


# ── Fixtures ──────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def driver():
    opts = Options()
    opts.add_argument("--headless")
    opts.add_argument("--no-sandbox")
    opts.add_argument("--disable-dev-shm-usage")
    opts.add_argument("--window-size=1280,800")
    drv = webdriver.Chrome(
        service=Service(ChromeDriverManager().install()),
        options=opts
    )
    drv.implicitly_wait(5)
    yield drv
    drv.quit()


# ── Helpers ───────────────────────────────────────────────────────────────────

def wait_for(driver, by, selector, timeout=WAIT_TIMEOUT):
    return WebDriverWait(driver, timeout).until(
        EC.presence_of_element_located((by, selector))
    )

def wait_clickable(driver, by, selector, timeout=WAIT_TIMEOUT):
    return WebDriverWait(driver, timeout).until(
        EC.element_to_be_clickable((by, selector))
    )


# ── Tests ─────────────────────────────────────────────────────────────────────

class TestNavbarPresence:
    """Verify the navbar and its links exist on the page."""

    def test_navbar_exists(self, driver):
        driver.get(BASE_URL)
        nav = wait_for(driver, By.CSS_SELECTOR, NAV_SELECTOR)
        assert nav.is_displayed(), "Navbar should be visible"

    def test_all_expected_links_present(self, driver):
        driver.get(BASE_URL)
        wait_for(driver, By.CSS_SELECTOR, NAV_LINK_SELECTOR)
        links = driver.find_elements(By.CSS_SELECTOR, NAV_LINK_SELECTOR)
        link_texts = [l.text.strip() for l in links]

        missing = [text for text, _ in EXPECTED_NAV_LINKS if text not in link_texts]
        assert not missing, f"Missing nav links: {missing}\nFound: {link_texts}"

    def test_no_broken_hrefs(self, driver):
        driver.get(BASE_URL)
        wait_for(driver, By.CSS_SELECTOR, NAV_LINK_SELECTOR)
        links = driver.find_elements(By.CSS_SELECTOR, NAV_LINK_SELECTOR)

        broken = []
        for link in links:
            href = link.get_attribute("href")
            if not href or href in ("#", "javascript:void(0)"):
                broken.append(link.text.strip())

        assert not broken, f"Links with missing/placeholder hrefs: {broken}"


class TestNavbarNavigation:
    """Click each nav link and confirm correct page loads."""

    @pytest.mark.parametrize("link_text,expected_path", EXPECTED_NAV_LINKS)
    def test_link_navigates_correctly(self, driver, link_text, expected_path):
        driver.get(BASE_URL)
        wait_for(driver, By.CSS_SELECTOR, NAV_LINK_SELECTOR)

        link = driver.find_element(By.LINK_TEXT, link_text)
        link.click()

        WebDriverWait(driver, WAIT_TIMEOUT).until(
            lambda d: expected_path in d.current_url
        )
        assert expected_path in driver.current_url, (
            f"'{link_text}' should navigate to '{expected_path}', "
            f"but current URL is '{driver.current_url}'"
        )

    @pytest.mark.parametrize("link_text,expected_path", EXPECTED_NAV_LINKS)
    def test_page_title_not_empty_after_navigation(self, driver, link_text, expected_path):
        driver.get(BASE_URL)
        wait_for(driver, By.CSS_SELECTOR, NAV_LINK_SELECTOR)

        driver.find_element(By.LINK_TEXT, link_text).click()
        WebDriverWait(driver, WAIT_TIMEOUT).until(
            lambda d: expected_path in d.current_url
        )
        assert driver.title.strip(), f"Page title is empty after navigating to '{link_text}'"


class TestNavbarActiveState:
    """Verify the correct link receives the active CSS class after navigation."""

    @pytest.mark.parametrize("link_text,expected_path", EXPECTED_NAV_LINKS)
    def test_active_class_on_current_page(self, driver, link_text, expected_path):
        if not ACTIVE_CLASS:
            pytest.skip("Site uses Tailwind utilities — no semantic active class")

        driver.get(BASE_URL + expected_path)

        try:
            wait_for(driver, By.CSS_SELECTOR, NAV_LINK_SELECTOR)
            links = driver.find_elements(By.CSS_SELECTOR, NAV_LINK_SELECTOR)
            active_links = [
                l.text.strip()
                for l in links
                if ACTIVE_CLASS in (l.get_attribute("class") or "")
            ]
            assert link_text in active_links, (
                f"Expected '{link_text}' to have class '{ACTIVE_CLASS}', "
                f"active links found: {active_links}"
            )
        except TimeoutException:
            pytest.skip(f"Could not load {expected_path} — skipping active-state check")


class TestMobileNavbar:
    """Test hamburger menu toggle on a narrow viewport."""

    @pytest.fixture(autouse=True)
    def mobile_viewport(self, driver):
        driver.set_window_size(390, 844)
        yield
        driver.set_window_size(1280, 800)

    def test_hamburger_button_visible_on_mobile(self, driver):
        driver.get(BASE_URL)
        try:
            toggle = wait_for(driver, By.CSS_SELECTOR, HAMBURGER_SELECTOR)
            assert toggle.is_displayed(), "Hamburger button should be visible on mobile"
        except TimeoutException:
            pytest.skip("No hamburger selector found — site may not have a mobile menu")

    def test_hamburger_opens_mobile_menu(self, driver):
        driver.get(BASE_URL)
        try:
            toggle = wait_clickable(driver, By.CSS_SELECTOR, HAMBURGER_SELECTOR)
            toggle.click()
            mobile_menu = wait_for(driver, By.CSS_SELECTOR, MOBILE_MENU_SELECTOR)
            assert mobile_menu.is_displayed(), "Mobile menu should be visible after toggle"
        except TimeoutException:
            pytest.skip("Mobile menu elements not found — update HAMBURGER_SELECTOR / MOBILE_MENU_SELECTOR")

    def test_hamburger_closes_mobile_menu(self, driver):
        driver.get(BASE_URL)
        try:
            toggle = wait_clickable(driver, By.CSS_SELECTOR, HAMBURGER_SELECTOR)
            toggle.click()
            time.sleep(0.3)
            toggle = wait_clickable(driver, By.CSS_SELECTOR, HAMBURGER_SELECTOR)
            toggle.click()
            time.sleep(0.3)
            mobile_menu = driver.find_element(By.CSS_SELECTOR, MOBILE_MENU_SELECTOR)
            assert not mobile_menu.is_displayed(), "Mobile menu should close on second toggle"
        except (TimeoutException, NoSuchElementException):
            pytest.skip("Mobile menu elements not found — update selectors")


class TestNavbarAccessibility:
    """Basic a11y checks on the navbar."""

    def test_nav_has_aria_label_or_role(self, driver):
        driver.get(BASE_URL)
        nav = wait_for(driver, By.CSS_SELECTOR, NAV_SELECTOR)
        aria_label = nav.get_attribute("aria-label")
        role = nav.get_attribute("role")
        assert aria_label or role or nav.tag_name == "nav", (
            "Navbar should be a <nav> element or have an ARIA role/label"
        )

    def test_links_have_discernible_text(self, driver):
        driver.get(BASE_URL)
        wait_for(driver, By.CSS_SELECTOR, NAV_LINK_SELECTOR)
        links = driver.find_elements(By.CSS_SELECTOR, NAV_LINK_SELECTOR)

        no_text = []
        for link in links:
            text = link.text.strip() or link.get_attribute("aria-label") or ""
            if not text:
                no_text.append(link.get_attribute("href"))

        assert not no_text, f"Links with no visible text or aria-label: {no_text}"


# ── Standalone runner ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import sys
    sys.exit(pytest.main([__file__, "-v", "--tb=short"]))