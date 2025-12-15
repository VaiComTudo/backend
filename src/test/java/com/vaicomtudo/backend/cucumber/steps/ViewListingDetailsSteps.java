package com.vaicomtudo.backend.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.AvailabilityPeriod;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ViewListingDetailsSteps {

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

    private WebDriverWait wait;
    private String renterEmail = "renter@test.com";
    private String ownerEmail = "owner@test.com";
    private String testPassword = "password123";
    private User renterUser;
    private User ownerUser;
    private Listing testListing;

    @Given("I am viewing the list of filtered bicycles")
    public void i_am_viewing_the_list_of_filtered_bicycles() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Clean up existing data
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create owner account
        Account ownerAccount = Account.builder()
            .email(ownerEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Test Owner")
            .build();
        ownerAccount = accountRepository.save(ownerAccount);

        ownerUser = User.builder()
            .account(ownerAccount)
            .birthdate(LocalDate.of(1985, 6, 15))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        ownerUser = userRepository.save(ownerUser);

        // Create renter account
        Account renterAccount = Account.builder()
            .email(renterEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Test Renter")
            .build();
        renterAccount = accountRepository.save(renterAccount);

        renterUser = User.builder()
            .account(renterAccount)
            .birthdate(LocalDate.of(1992, 3, 20))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        renterUser = userRepository.save(renterUser);

        // Create test listing with availability periods
        Set<AvailabilityPeriod> availability = new HashSet<>();
        AvailabilityPeriod weekdayPeriod = new AvailabilityPeriod();
        weekdayPeriod.setStartDay(DayOfWeek.MONDAY);
        weekdayPeriod.setEndDay(DayOfWeek.FRIDAY);
        weekdayPeriod.setStartTime(LocalTime.of(9, 0));
        weekdayPeriod.setEndTime(LocalTime.of(18, 0));
        availability.add(weekdayPeriod);

        AvailabilityPeriod weekendPeriod = new AvailabilityPeriod();
        weekendPeriod.setStartDay(DayOfWeek.SATURDAY);
        weekendPeriod.setEndDay(DayOfWeek.SUNDAY);
        weekendPeriod.setStartTime(LocalTime.of(10, 0));
        weekendPeriod.setEndTime(LocalTime.of(20, 0));
        availability.add(weekendPeriod);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("BICYCLE");
        vehicle.setCondition(VehicleCondition.GOOD);

        testListing = new Listing();
        testListing.setTitle("Premium Mountain Bike");
        testListing.setDescription("High-quality mountain bike perfect for trails and city rides. Recently serviced with new tires.");
        testListing.setPrice(new BigDecimal("25.00"));
        testListing.setPickUpLocation("Aveiro City Center");
        testListing.setDropOffLocation("Aveiro City Center");
        testListing.setOwner(ownerUser);
        testListing.setVehicle(vehicle);
        testListing.setState(ListingState.AVAILABLE);
        testListing.setAvailability(availability);
        testListing = listingRepository.save(testListing);

        // Navigate to frontend and login as renter
        driver.get(frontendUrl + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));

        WebElement emailInput = driver.findElement(By.id("login-email"));
        WebElement passwordInput = driver.findElement(By.id("login-password"));
        WebElement loginButton = driver.findElement(By.id("login-submit-btn"));

        emailInput.sendKeys(renterEmail);
        passwordInput.sendKeys(testPassword);
        loginButton.click();

        // Wait for redirect to explore page
        wait.until(ExpectedConditions.urlContains("/explore"));
        
        // Wait for listings to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("listing-" + testListing.getId())));
    }

    @When("I select a specific bicycle to view details")
    public void i_select_a_specific_bicycle_to_view_details() {
        // Click on the first listing card to view details
        WebElement listingCard = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("listing-" + testListing.getId()))
        );
        listingCard.click();

        // Wait for navigation to listing details page
        wait.until(ExpectedConditions.urlContains("/listing/"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("listing-details-container")));
    }

    @Then("the system displays comprehensive information including photos, description, and pickup location")
    public void the_system_displays_comprehensive_information_including_photos_description_and_pickup_location() {
        // Verify listing details container is displayed
        WebElement detailsContainer = driver.findElement(By.id("listing-details-container"));
        assertTrue(detailsContainer.isDisplayed(), "Listing details container should be displayed");

        // Verify title
        WebElement title = driver.findElement(By.id("listing-details-title"));
        assertEquals("Premium Mountain Bike", title.getText(), "Title should match");

        // Verify price
        WebElement price = driver.findElement(By.id("listing-details-price"));
        assertTrue(price.getText().contains("25"), "Price should be displayed");

        // Verify description section
        WebElement descriptionSection = driver.findElement(By.id("listing-details-description-section"));
        assertTrue(descriptionSection.isDisplayed(), "Description section should be displayed");

        WebElement descriptionText = driver.findElement(By.id("listing-details-description-text"));
        assertTrue(
            descriptionText.getText().contains("High-quality mountain bike"),
            "Description text should match"
        );

        // Verify pickup location
        WebElement pickupLocationCard = driver.findElement(By.id("listing-details-pickup-location-card"));
        assertTrue(pickupLocationCard.isDisplayed(), "Pickup location card should be displayed");

        WebElement pickupLocationText = driver.findElement(By.id("listing-details-pickup-location-text"));
        assertEquals("Aveiro City Center", pickupLocationText.getText(), "Pickup location should match");

        // Verify dropoff location
        WebElement dropoffLocationCard = driver.findElement(By.id("listing-details-dropoff-location-card"));
        assertTrue(dropoffLocationCard.isDisplayed(), "Dropoff location card should be displayed");

        WebElement dropoffLocationText = driver.findElement(By.id("listing-details-dropoff-location-text"));
        assertEquals("Aveiro City Center", dropoffLocationText.getText(), "Dropoff location should match");

        // Verify image container (may have image or placeholder)
        WebElement imageContainer = driver.findElement(By.id("listing-details-image-container"));
        assertTrue(imageContainer.isDisplayed(), "Image container should be displayed");

        // Verify vehicle information
        WebElement vehicleSection = driver.findElement(By.id("listing-details-vehicle-section"));
        assertTrue(vehicleSection.isDisplayed(), "Vehicle section should be displayed");

        WebElement vehicleTypeValue = driver.findElement(By.id("listing-details-vehicle-type-value"));
        assertEquals("BICYCLE", vehicleTypeValue.getText(), "Vehicle type should match");

        WebElement vehicleConditionValue = driver.findElement(By.id("listing-details-vehicle-condition-value"));
        assertEquals("GOOD", vehicleConditionValue.getText(), "Vehicle condition should match");
    }

    @Then("I can see the item's availability calendar")
    public void i_can_see_the_items_availability_calendar() {
        // Verify availability section is displayed
        WebElement availabilitySection = driver.findElement(By.id("listing-details-availability-section"));
        assertTrue(availabilitySection.isDisplayed(), "Availability section should be displayed");

        WebElement availabilityHeading = driver.findElement(By.id("listing-details-availability-heading"));
        assertEquals("Availability", availabilityHeading.getText(), "Availability heading should match");

        // Verify availability list is displayed
        WebElement availabilityList = driver.findElement(By.id("listing-details-availability-list"));
        assertTrue(availabilityList.isDisplayed(), "Availability list should be displayed");

        // Verify at least one availability period is displayed
        List<WebElement> availabilityPeriods = driver.findElements(
            By.cssSelector("[id^='listing-details-availability-period-']")
        );
        assertTrue(availabilityPeriods.size() >= 1, "At least one availability period should be displayed");

        // Verify first period has days and time information
        WebElement firstPeriodDays = driver.findElement(By.id("listing-details-availability-days-0"));
        assertNotNull(firstPeriodDays.getText(), "Availability days should be displayed");
        assertTrue(
            firstPeriodDays.getText().contains("Monday") || firstPeriodDays.getText().contains("Saturday"),
            "Days should include Monday or Saturday"
        );

        WebElement firstPeriodTime = driver.findElement(By.id("listing-details-availability-time-0"));
        assertNotNull(firstPeriodTime.getText(), "Availability time should be displayed");
        assertTrue(
            firstPeriodTime.getText().contains(":"),
            "Time should be in time format"
        );
    }

    @Then("I have the option to proceed with booking")
    public void i_have_the_option_to_proceed_with_booking() {
        // Verify book button is displayed
        WebElement bookButton = driver.findElement(By.id("listing-details-book-button"));
        assertTrue(bookButton.isDisplayed(), "Book button should be displayed");
        assertTrue(bookButton.isEnabled(), "Book button should be enabled");
        assertEquals("Book Now", bookButton.getText(), "Book button text should match");

        // Verify back button is also available
        WebElement backButton = driver.findElement(By.id("listing-details-back-button"));
        assertTrue(backButton.isDisplayed(), "Back button should be displayed");
        assertTrue(backButton.getText().contains("Back to Explore"), "Back button text should match");
    }
}
