package com.example.demo;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginTest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private static final String BASE_URL  = "https://smartpark-reservation-frontend.vercel.app";
    private static final String LOGIN_URL = BASE_URL + "/login";

    private static final String VALID_EMAIL    = "rehenu@gmail.com";
    private static final String VALID_PASSWORD = "Rehenu";

    private static final String ADMIN_EMAIL    = "chama.smartpark@gmail.com";
    private static final String ADMIN_PASSWORD = "Chama";

    @BeforeAll
    static void setUp() {
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
        wait   = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) driver.quit();
    }

    private void openLoginPage() {
        driver.get(LOGIN_URL);
        wait.until(ExpectedConditions.urlContains("/login"));
    }

    private WebElement waitFor(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private void fillField(By locator, String value) {
        WebElement el = waitFor(locator);
        el.clear();
        el.sendKeys(value);
    }

    private static final By EMAIL_INPUT    = By.xpath("//input[@placeholder='Email']");
    private static final By PASSWORD_INPUT = By.xpath("//input[@placeholder='Password']");
    private static final By SIGN_IN_BUTTON = By.xpath("//button[@type='submit' and normalize-space()='Sign In']");
    private static final By ERROR_MESSAGE  = By.xpath("//p[contains(@class,'text-red-500')]");
    private static final By FORGOT_PW_LINK = By.xpath("//span[normalize-space()='Forgot password?']");
    private static final By SIGN_UP_LINK   = By.xpath("//span[normalize-space()='Sign up']");
    private static final By BACK_BUTTON    = By.xpath("//button[normalize-space()='← Back']");

    @Test
    @Order(1)
    @DisplayName("Login page loads correctly")
    void testPageLoads() {
        openLoginPage();
        assertEquals(LOGIN_URL, driver.getCurrentUrl());
        assertTrue(waitFor(SIGN_IN_BUTTON).isDisplayed());
        System.out.println("✔ TC01 PASSED – Login page loads correctly");
    }

    @Test
    @Order(2)
    @DisplayName("All form fields are visible")
    void testFormFieldsVisible() {
        openLoginPage();
        assertTrue(waitFor(EMAIL_INPUT).isDisplayed());
        assertTrue(waitFor(PASSWORD_INPUT).isDisplayed());
        System.out.println("✔ TC02 PASSED – Email and Password fields are visible");
    }

    @Test
    @Order(3)
    @DisplayName("Submit with empty fields shows validation error")
    void testEmptyFieldsValidation() {
        openLoginPage();
        waitFor(SIGN_IN_BUTTON).click();
        String error = waitFor(ERROR_MESSAGE).getText();
        assertEquals("Please fill in all fields.", error);
        System.out.println("✔ TC03 PASSED – Empty fields show: \"" + error + "\"");
    }

    @Test
    @Order(4)
    @DisplayName("Wrong password shows error")
    void testWrongPassword() {
        openLoginPage();
        fillField(EMAIL_INPUT,    VALID_EMAIL);
        fillField(PASSWORD_INPUT, "WrongPassword!");
        waitFor(SIGN_IN_BUTTON).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_MESSAGE));
        assertFalse(error.getText().isEmpty());
        System.out.println("✔ TC04 PASSED – Wrong password shows error: \"" + error.getText() + "\"");
    }

    @Test
    @Order(5)
    @DisplayName("Non-existent email shows error")
    void testNonExistentEmail() {
        openLoginPage();
        fillField(EMAIL_INPUT,    "nobody_" + System.currentTimeMillis() + "@mail.com");
        fillField(PASSWORD_INPUT, "SomePass1!");
        waitFor(SIGN_IN_BUTTON).click();
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_MESSAGE));
        assertFalse(error.getText().isEmpty());
        System.out.println("✔ TC05 PASSED – Non-existent email shows error: \"" + error.getText() + "\"");
    }

    @Test
    @Order(6)
    @DisplayName("Successful COMMUTER login redirects to '/'")
    void testSuccessfulCommuterLogin() {
        openLoginPage();
        fillField(EMAIL_INPUT,    VALID_EMAIL);
        fillField(PASSWORD_INPUT, VALID_PASSWORD);
        waitFor(SIGN_IN_BUTTON).click();
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/dashboard/attendant"));
        assertEquals(BASE_URL + "/dashboard/attendant", driver.getCurrentUrl());
        ((JavascriptExecutor) driver).executeScript("localStorage.removeItem('user');");
        System.out.println("✔ TC06 PASSED – Commuter login redirected to '/dashboard/attendant'");
    }

    @Test
    @Order(7)
    @DisplayName("Successful ADMIN login redirects to /dashboard")
    void testSuccessfulAdminLogin() {
        openLoginPage();
        fillField(EMAIL_INPUT,    ADMIN_EMAIL);
        fillField(PASSWORD_INPUT, ADMIN_PASSWORD);
        waitFor(SIGN_IN_BUTTON).click();
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        assertTrue(driver.getCurrentUrl().contains("/dashboard"));
        ((JavascriptExecutor) driver).executeScript("localStorage.removeItem('user');");
        System.out.println("✔ TC07 PASSED – Admin login redirected to /dashboard");
    }

    @Test
    @Order(8)
    @DisplayName("'Forgot password?' link navigates to /forgot-password")
    void testForgotPasswordLink() {
        openLoginPage();
        waitFor(FORGOT_PW_LINK).click();
        wait.until(ExpectedConditions.urlContains("/forgot-password"));
        assertTrue(driver.getCurrentUrl().contains("/forgot-password"));
        System.out.println("✔ TC08 PASSED – 'Forgot password?' navigates to /forgot-password");
    }

    @Test
    @Order(9)
    @DisplayName("'Sign up' link navigates to /signup")
    void testSignUpLink() {
        openLoginPage();
        waitFor(SIGN_UP_LINK).click();
        wait.until(ExpectedConditions.urlContains("/signup"));
        assertTrue(driver.getCurrentUrl().contains("/signup"));
        System.out.println("✔ TC09 PASSED – 'Sign up' link navigates to /signup");
    }

    @Test
    @Order(10)
    @DisplayName("Back button navigates to home '/'")
    void testBackButton() {
        openLoginPage();
        waitFor(BACK_BUTTON).click();
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/"));
        assertEquals(BASE_URL + "/", driver.getCurrentUrl());
        System.out.println("✔ TC10 PASSED – Back button navigates to '/'");
    }

    @Test
    @Order(11)
    @DisplayName("Enter key submits the login form")
    void testEnterKeySubmitsForm() {
        openLoginPage();
        fillField(EMAIL_INPUT,    VALID_EMAIL);
        fillField(PASSWORD_INPUT, VALID_PASSWORD);
        waitFor(PASSWORD_INPUT).sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/dashboard/attendant"));
        assertEquals(BASE_URL + "/dashboard/attendant", driver.getCurrentUrl());
        ((JavascriptExecutor) driver).executeScript("localStorage.removeItem('user');");
        System.out.println("✔ TC11 PASSED – Enter key submits form and redirects to '/dashboard/attendant'");
    }
}