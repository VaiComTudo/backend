package com.vaicomtudo.backend.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.PaymentRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CompletePaymentTransactionSteps {

    @Autowired
    private WebDriver driver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${frontend.url}")
    private String frontendUrl;

    private WebDriverWait wait;
    private Listing approvedBookingListing;
    private String renterEmail = "renter-payment@test.com";
    private String ownerEmail = "owner-payment@test.com";
    private String testPassword = "password123";

    @Given("my booking has been approved by the owner")
    public void my_booking_has_been_approved_by_the_owner() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Limpar dados anteriores
        // Ordem importante devido à FK fk_payment_renter (payment -> users)
        paymentRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Criar owner
        Account ownerAccount = Account.builder()
                .email(ownerEmail)
                .passwordHash(passwordEncoder.encode(testPassword))
                .name("Owner Payment")
                .build();
        ownerAccount = accountRepository.save(ownerAccount);

        User ownerUser = User.builder()
                .account(ownerAccount)
                .birthdate(LocalDate.of(1980, 1, 1))
                .rating(0.0)
                .role(Role.NORMAL_USER)
                .build();
        ownerUser = userRepository.save(ownerUser);

        // Criar renter
        Account renterAccount = Account.builder()
                .email(renterEmail)
                .passwordHash(passwordEncoder.encode(testPassword))
                .name("Renter Payment")
                .build();
        renterAccount = accountRepository.save(renterAccount);

        User renterUser = User.builder()
                .account(renterAccount)
                .birthdate(LocalDate.of(1990, 5, 10))
                .rating(0.0)
                .role(Role.NORMAL_USER)
                .build();
        renterUser = userRepository.save(renterUser);

        // Criar uma listing "aprovada" (AVAILABLE) que representará a reserva aprovada
        Listing listing = new Listing();
        listing.setTitle("City Bike for Payment Test");
        listing.setDescription("Bike used to test renter payment transaction.");
        listing.setPrice(new BigDecimal("30.00"));
        listing.setPickUpLocation("Aveiro City Center");
        listing.setDropOffLocation("Aveiro City Center");
        listing.setOwner(ownerUser);
        listing.setState(ListingState.AVAILABLE);

        approvedBookingListing = listingRepository.save(listing);

        // Login como renter no frontend
        driver.get(frontendUrl + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));

        WebElement emailInput = driver.findElement(By.id("login-email"));
        WebElement passwordInput = driver.findElement(By.id("login-password"));
        WebElement loginButton = driver.findElement(By.id("login-submit-btn"));

        emailInput.sendKeys(renterEmail);
        passwordInput.sendKeys(testPassword);
        loginButton.click();

        // Esperar redireção para /explore
        wait.until(ExpectedConditions.urlContains("/explore"));

        // Ir diretamente para a página de detalhes da listing aprovada
        driver.get(frontendUrl + "/listing/" + approvedBookingListing.getId());
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("listing-details-container")));
    }

    @When("I proceed to checkout")
    public void i_proceed_to_checkout() {
        WebElement bookButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("listing-details-book-button")));
        bookButton.click();

        wait.until(ExpectedConditions.urlContains("/checkout/"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("checkout-page")));
    }

    @And("complete payment via the payment system")
    public void complete_payment_via_the_payment_system() {
        WebElement completePaymentButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("checkout-complete-payment-button")));
        completePaymentButton.click();

        // Esperar mensagem de sucesso
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("checkout-success-message")));
    }

    @Then("the payment is securely processed including the rental fee and deposit")
    public void the_payment_is_securely_processed_including_the_rental_fee_and_deposit() {
        WebElement rentalFeeElement = driver.findElement(By.id("checkout-rental-fee-amount"));
        WebElement depositElement = driver.findElement(By.id("checkout-deposit-amount"));
        WebElement totalElement = driver.findElement(By.id("checkout-total-amount"));

        String rentalFeeText = rentalFeeElement.getText(); // e.g., "€30"
        String depositText = depositElement.getText(); // e.g., "€50"
        String totalText = totalElement.getText(); // e.g., "€80"

        assertTrue(rentalFeeText.contains("30"), "Rental fee should be 30");
        assertTrue(depositText.contains("50"), "Deposit should be 50");
        assertTrue(totalText.contains("80"), "Total should be 80 (30 + 50)");
    }

    @And("I receive a digital receipt via email")
    public void i_receive_a_digital_receipt_via_email() {
        WebElement receiptMessage = driver.findElement(By.id("checkout-receipt-message"));
        String text = receiptMessage.getText();

        assertTrue(
                text.toLowerCase().contains("receipt") || text.toLowerCase().contains("recibo"),
                "Receipt message should mention a receipt being sent");
    }

    @And("my booking status is updated to {string}")
    public void my_booking_status_is_updated_to(String expectedStatus) {
        WebElement statusElement = driver.findElement(By.id("checkout-booking-status"));
        String statusText = statusElement.getText().toLowerCase();

        assertEquals(expectedStatus.toLowerCase(), statusText, "Booking status should match expected status");
    }
}
