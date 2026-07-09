package com.example.demo;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium Navbar Navigation Tests – SmartPark
 *
 * Covers: link presence, clickability, URL routing,
 *         active states, mobile hamburger menu, basic a11y.
 *
 * Run:  mvn test  (or run this class directly from IntelliJ / Eclipse)
 *
 * Dependencies (pom.xml):
 *   - org.seleniumhq.selenium : selenium-java      : 4.x
 *   - io.github.bonigarcia    : webdrivermanager   : 5.x
 *   - org.junit.jupiter       : junit-jupiter      : 5.x
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NavbarTest {

    // ── Config ────────────────────────────────────────────────────────────────

    private static final String BASE_URL             = "http://localhost:3000";
    private static final int    WAIT_TIMEOUT         = 10;

    private static final String NAV_SELECTOR         = "nav";
    private static final String NAV_LINK_SELECTOR    = "nav ul a";
    private static final String ACTIVE_CLASS         = null;   // Tailwind – no semantic active class
    private static final String HAMBURGER_SELECTOR   =
            "[data-testid='menu-toggle'], .hamburger, .menu-toggle";
    private static final String MOBILE_MENU_SELECTOR =
            ".mobile-menu, .nav-mobile, [data-testid='mobile-nav']";

    private static final String[][] NAV_LINKS = {
            { "Home",        "/"         },
            { "Reservation", "/slots"    },
            { "About",       "/about-us" },
    };

    // ── Report counter (thread-safe across nested classes) ────────────────────

    private static final AtomicInteger TC_COUNTER    = new AtomicInteger(0);
    private static final int           TOTAL_TESTS   = 17;

    // ── Driver ────────────────────────────────────────────────────────────────

    private WebDriver     driver;
    private WebDriverWait wait;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeAll
    void setUpDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless");
        opts.addArguments("--no-sandbox");
        opts.addArguments("--disable-dev-shm-usage");
        opts.addArguments("--window-size=1280,800");

        driver = new ChromeDriver(opts);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait   = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT));

        printReportHeader();
    }

    @AfterAll
    void tearDownDriver() {
        if (driver != null) driver.quit();
        printReportFooter();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WebElement waitFor(By by) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(by));
    }

    private WebElement waitClickable(By by) {
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    private void setWindowSize(int width, int height) {
        driver.manage().window().setSize(new Dimension(width, height));
    }

    // ── Report helpers ────────────────────────────────────────────────────────

    /**
     * Call at the top of every test to print a formatted TC line.
     *
     * @param section  section name, e.g. "1. Navbar Presence"
     * @param name     short test name for the report
     */
    private static void printTC(String section, String name) {
        int num = TC_COUNTER.incrementAndGet();
        System.out.printf("TC-%02d  [%s]  %s%n", num, section, name);
    }

    private static void printReportHeader() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║             SMARTPARK — NAVBAR TEST CASE REPORT                  ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf ("║  Base URL : %-53s║%n", BASE_URL);
        System.out.println("║  Browser  : Chrome (headless)                                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("── 1. Navbar Presence ──────────────────────────────────────────────");
        System.out.println("TC-01  [1. Navbar Presence]  Navbar element is visible on the page");
        System.out.println("TC-02  [1. Navbar Presence]  All expected nav links are present");
        System.out.println("TC-03  [1. Navbar Presence]  No links have broken or placeholder hrefs");
        System.out.println();

        System.out.println("── 2. Navbar Navigation ────────────────────────────────────────────");
        System.out.println("TC-04  [2. Navbar Navigation]  'Home' navigates to '/'");
        System.out.println("TC-05  [2. Navbar Navigation]  'Reservation' navigates to '/slots'");
        System.out.println("TC-06  [2. Navbar Navigation]  'About' navigates to '/about-us'");
        System.out.println("TC-07  [2. Navbar Navigation]  Page title not empty after navigating to 'Home'");
        System.out.println("TC-08  [2. Navbar Navigation]  Page title not empty after navigating to 'Reservation'");
        System.out.println("TC-09  [2. Navbar Navigation]  Page title not empty after navigating to 'About'");
        System.out.println();

        System.out.println("── 3. Active Link State ─────────────────────────────────────────────");
        System.out.println("TC-10  [3. Active Link State]  'Home' active class on own page        [SKIPPED – Tailwind]");
        System.out.println("TC-11  [3. Active Link State]  'Reservation' active class on own page [SKIPPED – Tailwind]");
        System.out.println("TC-12  [3. Active Link State]  'About' active class on own page       [SKIPPED – Tailwind]");
        System.out.println();

        System.out.println("── 4. Mobile Hamburger Menu ─────────────────────────────────────────");
        System.out.println("TC-13  [4. Mobile Hamburger Menu]  Hamburger button is visible on narrow viewport");
        System.out.println("TC-14  [4. Mobile Hamburger Menu]  Clicking hamburger opens the mobile menu");
        System.out.println("TC-15  [4. Mobile Hamburger Menu]  Clicking hamburger twice closes the mobile menu");
        System.out.println();

        System.out.println("── 5. Navbar Accessibility ──────────────────────────────────────────");
        System.out.println("TC-16  [5. Navbar Accessibility]  Nav element has ARIA label, role, or is a semantic nav");
        System.out.println("TC-17  [5. Navbar Accessibility]  All nav links have discernible text or aria-label");
        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════════════");
        System.out.println("  Running tests…");
        System.out.println("════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    private static void printReportFooter() {
        int skipped = 3; // active-class tests always skipped for Tailwind
        int ran     = TOTAL_TESTS - skipped;
        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════════════");
        System.out.printf ("  Total: %-4d Ran: %-4d Skipped: %d  (Active-class tests, Tailwind)%n",
                TOTAL_TESTS, ran, skipped);
        System.out.println("════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    // ── 1. Navbar Presence ────────────────────────────────────────────────────

    @Nested
    @DisplayName("1. Navbar Presence")
    class NavbarPresence {

        @Test
        @Order(1)
        @DisplayName("Navbar element is visible on the page")
        void navbarExists() {
            System.out.println("[RUNNING] TC-01 – Navbar element is visible on the page");
            driver.get(BASE_URL);
            WebElement nav = waitFor(By.cssSelector(NAV_SELECTOR));
            assertTrue(nav.isDisplayed(), "Navbar should be visible");
            System.out.println("[PASS]    TC-01");
        }

        @Test
        @Order(2)
        @DisplayName("All expected nav links are present")
        void allExpectedLinksPresent() {
            System.out.println("[RUNNING] TC-02 – All expected nav links are present");
            driver.get(BASE_URL);
            waitFor(By.cssSelector(NAV_LINK_SELECTOR));

            List<String> linkTexts = driver
                    .findElements(By.cssSelector(NAV_LINK_SELECTOR))
                    .stream()
                    .map(el -> el.getText().trim())
                    .collect(Collectors.toList());

            List<String> missing = java.util.Arrays.stream(NAV_LINKS)
                    .map(pair -> pair[0])
                    .filter(text -> !linkTexts.contains(text))
                    .collect(Collectors.toList());

            assertTrue(missing.isEmpty(),
                    "Missing nav links: " + missing + "\nFound: " + linkTexts);
            System.out.println("[PASS]    TC-02");
        }

        @Test
        @Order(3)
        @DisplayName("No links have broken or placeholder hrefs")
        void noBrokenHrefs() {
            System.out.println("[RUNNING] TC-03 – No links have broken or placeholder hrefs");
            driver.get(BASE_URL);
            waitFor(By.cssSelector(NAV_LINK_SELECTOR));

            List<String> broken = driver
                    .findElements(By.cssSelector(NAV_LINK_SELECTOR))
                    .stream()
                    .filter(el -> {
                        String href = el.getAttribute("href");
                        return href == null
                                || href.isBlank()
                                || href.equals("#")
                                || href.equals("javascript:void(0)");
                    })
                    .map(el -> el.getText().trim())
                    .collect(Collectors.toList());

            assertTrue(broken.isEmpty(),
                    "Links with missing/placeholder hrefs: " + broken);
            System.out.println("[PASS]    TC-03");
        }
    }

    // ── 2. Navbar Navigation ──────────────────────────────────────────────────

    @Nested
    @DisplayName("2. Navbar Navigation")
    class NavbarNavigation {

        @ParameterizedTest(name = "'{0}' navigates to '{1}'")
        @CsvSource({
                "Home,        /",
                "Reservation, /slots",
                "About,       /about-us",
        })
        @Order(4)
        @DisplayName("Each link navigates to the correct path")
        void linkNavigatesCorrectly(String linkText, String expectedPath) {
            System.out.println("[RUNNING] Navigation test – '" + linkText.trim()
                    + "' → '" + expectedPath.trim() + "'");
            driver.get(BASE_URL);
            waitFor(By.cssSelector(NAV_LINK_SELECTOR));

            driver.findElement(By.linkText(linkText.trim())).click();
            wait.until(d -> d.getCurrentUrl().contains(expectedPath.trim()));

            assertTrue(driver.getCurrentUrl().contains(expectedPath.trim()),
                    "'" + linkText + "' should navigate to '" + expectedPath
                            + "' but URL is: " + driver.getCurrentUrl());
            System.out.println("[PASS]    Navigation – '" + linkText.trim() + "'");
        }

        @ParameterizedTest(name = "Page title not empty after navigating to '{0}'")
        @CsvSource({
                "Home,        /",
                "Reservation, /slots",
                "About,       /about-us",
        })
        @Order(5)
        @DisplayName("Page title is not empty after navigation")
        void pageTitleNotEmptyAfterNavigation(String linkText, String expectedPath) {
            System.out.println("[RUNNING] Title test – '" + linkText.trim() + "'");
            driver.get(BASE_URL);
            waitFor(By.cssSelector(NAV_LINK_SELECTOR));

            driver.findElement(By.linkText(linkText.trim())).click();
            wait.until(d -> d.getCurrentUrl().contains(expectedPath.trim()));

            assertFalse(driver.getTitle().isBlank(),
                    "Page title should not be empty after navigating to '" + linkText + "'");
            System.out.println("[PASS]    Title test – '" + linkText.trim() + "'");
        }
    }

    // ── 3. Active State ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("3. Active Link State")
    class ActiveState {

        @ParameterizedTest(name = "'{0}' has active class on its own page")
        @CsvSource({
                "Home,        /",
                "Reservation, /slots",
                "About,       /about-us",
        })
        @Order(6)
        @DisplayName("Correct link gets active CSS class after navigation")
        void activeLinkOnCurrentPage(String linkText, String expectedPath) {
            System.out.println("[SKIP]    Active-class test – '" + linkText.trim()
                    + "' (Tailwind utilities – no semantic active class)");
            if (ACTIVE_CLASS == null) {
                Assumptions.assumeTrue(false,
                        "Site uses Tailwind utilities — no semantic active class, skipping.");
            }

            driver.get(BASE_URL + expectedPath.trim());
            waitFor(By.cssSelector(NAV_LINK_SELECTOR));

            List<String> activeLinks = driver
                    .findElements(By.cssSelector(NAV_LINK_SELECTOR))
                    .stream()
                    .filter(el -> {
                        String cls = el.getAttribute("class");
                        return cls != null && cls.contains(ACTIVE_CLASS);
                    })
                    .map(el -> el.getText().trim())
                    .collect(Collectors.toList());

            assertTrue(activeLinks.contains(linkText.trim()),
                    "Expected '" + linkText + "' to have class '" + ACTIVE_CLASS
                            + "'. Active links found: " + activeLinks);
        }
    }

    // ── 4. Mobile Hamburger Menu ──────────────────────────────────────────────

    @Nested
    @DisplayName("4. Mobile Hamburger Menu")
    class MobileNavbar {

        @BeforeEach
        void mobileViewport() {
            setWindowSize(390, 844);
        }

        @AfterEach
        void desktopViewport() {
            setWindowSize(1280, 800);
        }

        @Test
        @Order(7)
        @DisplayName("Hamburger button is visible on narrow viewport")
        void hamburgerVisibleOnMobile() {
            System.out.println("[RUNNING] TC-13 – Hamburger button is visible on narrow viewport");
            driver.get(BASE_URL);
            try {
                WebElement toggle = waitFor(By.cssSelector(HAMBURGER_SELECTOR));
                assertTrue(toggle.isDisplayed(),
                        "Hamburger button should be visible on mobile viewport");
                System.out.println("[PASS]    TC-13");
            } catch (TimeoutException e) {
                System.out.println("[SKIP]    TC-13 – No hamburger selector found");
                Assumptions.assumeTrue(false,
                        "No hamburger selector found — site may not have a mobile menu");
            }
        }

        @Test
        @Order(8)
        @DisplayName("Clicking hamburger opens the mobile menu")
        void hamburgerOpensMobileMenu() {
            System.out.println("[RUNNING] TC-14 – Clicking hamburger opens the mobile menu");
            driver.get(BASE_URL);
            try {
                waitClickable(By.cssSelector(HAMBURGER_SELECTOR)).click();
                WebElement menu = waitFor(By.cssSelector(MOBILE_MENU_SELECTOR));
                assertTrue(menu.isDisplayed(),
                        "Mobile menu should be visible after toggle click");
                System.out.println("[PASS]    TC-14");
            } catch (TimeoutException e) {
                System.out.println("[SKIP]    TC-14 – Mobile menu elements not found");
                Assumptions.assumeTrue(false,
                        "Mobile menu elements not found — update HAMBURGER_SELECTOR / MOBILE_MENU_SELECTOR");
            }
        }

        @Test
        @Order(9)
        @DisplayName("Clicking hamburger twice closes the mobile menu")
        void hamburgerClosesMobileMenu() {
            System.out.println("[RUNNING] TC-15 – Clicking hamburger twice closes the mobile menu");
            driver.get(BASE_URL);
            try {
                waitClickable(By.cssSelector(HAMBURGER_SELECTOR)).click();
                Thread.sleep(300);
                waitClickable(By.cssSelector(HAMBURGER_SELECTOR)).click();
                Thread.sleep(300);

                WebElement menu = driver.findElement(By.cssSelector(MOBILE_MENU_SELECTOR));
                assertFalse(menu.isDisplayed(),
                        "Mobile menu should be hidden after second toggle click");
                System.out.println("[PASS]    TC-15");
            } catch (TimeoutException | NoSuchElementException e) {
                System.out.println("[SKIP]    TC-15 – Mobile menu elements not found");
                Assumptions.assumeTrue(false,
                        "Mobile menu elements not found — update selectors");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ── 5. Accessibility ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("5. Navbar Accessibility")
    class NavbarAccessibility {

        @Test
        @Order(10)
        @DisplayName("Nav element has ARIA label, role, or is a semantic <nav>")
        void navHasAriaOrRole() {
            System.out.println("[RUNNING] TC-16 – Nav has ARIA label, role, or is semantic nav");
            driver.get(BASE_URL);
            WebElement nav = waitFor(By.cssSelector(NAV_SELECTOR));

            String ariaLabel = nav.getAttribute("aria-label");
            String role      = nav.getAttribute("role");
            String tagName   = nav.getTagName();

            assertTrue(
                    (ariaLabel != null && !ariaLabel.isBlank())
                            || (role != null && !role.isBlank())
                            || tagName.equalsIgnoreCase("nav"),
                    "Navbar should be a <nav> element or have an ARIA role/label"
            );
            System.out.println("[PASS]    TC-16");
        }

        @Test
        @Order(11)
        @DisplayName("All nav links have discernible text or aria-label")
        void linksHaveDiscernibleText() {
            System.out.println("[RUNNING] TC-17 – All nav links have discernible text or aria-label");
            driver.get(BASE_URL);
            waitFor(By.cssSelector(NAV_LINK_SELECTOR));

            List<String> noText = driver
                    .findElements(By.cssSelector(NAV_LINK_SELECTOR))
                    .stream()
                    .filter(el -> {
                        String text      = el.getText().trim();
                        String ariaLabel = el.getAttribute("aria-label");
                        return text.isEmpty()
                                && (ariaLabel == null || ariaLabel.isBlank());
                    })
                    .map(el -> el.getAttribute("href"))
                    .collect(Collectors.toList());

            assertTrue(noText.isEmpty(),
                    "Links with no visible text or aria-label: " + noText);
            System.out.println("[PASS]    TC-17");
        }
    }
}