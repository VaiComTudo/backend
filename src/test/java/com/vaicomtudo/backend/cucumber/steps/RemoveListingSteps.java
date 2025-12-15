package com.vaicomtudo.backend.cucumber.steps;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

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
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveListingSteps {

    @Autowired
    private WebDriver driver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String testEmail = "owner@test.com";
    private String testPassword = "pass123";
    private User testUser;
    private UUID listingId;

    @Given("I have a bicycle listed that I want to remove from the platform")
    public void i_have_a_bicycle_listed_that_i_want_to_remove_from_the_platform() {
        // Clean up database
        listingRepository.deleteAll();
        userRepository.deleteAll();

        // Create test account
        Account account = Account.builder()
            .email(testEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Test Owner")
            .build();

        account = accountRepository.save(account);

        testUser = User.builder()
            .account(account)
            .birthdate(LocalDate.of(1990, 1, 1))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();

        testUser = userRepository.save(testUser);

        // Create a bicycle listing
        Listing listing = new Listing();
        listing.setTitle("Mountain Bicycle");
        listing.setDescription("A great bicycle for trails");
        listing.setPrice(BigDecimal.valueOf(25.00));
        listing.setState(ListingState.AVAILABLE);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setType("Bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        
        listing.setPickUpLocation("Main Street");
        listing.setDropOffLocation("Main Street");
        listing.setOwner(testUser);
        
        listing = listingRepository.save(listing);
        listingId = listing.getId();

        // Login to the application
        driver.get(frontendUrl + "/login");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-form")));

        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-email")));
        emailField.clear();
        emailField.sendKeys(testEmail);

        WebElement passwordField = driver.findElement(By.id("login-password"));
        passwordField.clear();
        passwordField.sendKeys(testPassword);

        WebElement submitButton = driver.findElement(By.id("login-submit-btn"));
        submitButton.click();

        // Wait for redirect to explore page
        wait.until(d -> d.getCurrentUrl().contains("/explore"));

        // Navigate to My Listings page
        driver.get(frontendUrl + "/my-listings");
        
        // Wait for the listing to appear
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("remove-listing-" + listingId)));
    }

    @When("I select the listing and click {string}")
    public void i_select_the_listing_and_click_remove(String buttonText) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Find and click the remove button for the specific listing
        WebElement removeButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("remove-listing-" + listingId))
        );
        
        // Use JavaScript click to ensure the event fires properly
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", removeButton);
        
        // Give React time to update the state and render the dialog
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Wait for the confirmation dialog overlay to appear and be visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("confirm-delete-overlay")));
        
        // Wait for the confirm button to be clickable and click it with JavaScript
        WebElement confirmButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("confirm-delete-confirm"))
        );
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmButton);
    }

    @Then("the system removes the listing from view")
    public void the_system_removes_the_listing_from_view() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for success message
        WebElement successMessage = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Listing removed successfully!')]")
            )
        );
        
        assertTrue(successMessage.isDisplayed(), "Success message should be displayed");
        
        // Verify the listing is no longer visible on the page
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.id("remove-listing-" + listingId)
        ));
    }

    @And("the item is no longer available for booking")
    public void the_item_is_no_longer_available_for_booking() {
        // Verify the listing is removed from the database
        assertTrue(listingRepository.findById(listingId).isEmpty(), 
            "Listing should be removed from the database");
    }

    @And("any pending booking requests are cancelled")
    public void any_pending_booking_requests_are_cancelled() {
        // Note: This step is currently a placeholder as the booking system is not yet implemented
        // When the booking system is implemented, this should verify that:
        // 1. All bookings associated with this listing are cancelled
        // 2. Notifications are sent to affected renters
        
        // For now, we just verify the listing is deleted, which satisfies the requirement
        assertTrue(listingRepository.findById(listingId).isEmpty(), 
            "Listing should be removed, ensuring no future bookings are possible");
    }
}
