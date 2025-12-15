package com.vaicomtudo.backend.cucumber.steps;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.AvailabilityPeriod;
import com.vaicomtudo.backend.data.entity.Booking;
import com.vaicomtudo.backend.data.entity.BookingState;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.BookingRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingSteps {

    @Autowired
    private WebDriver driver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String renterEmail = "renter@test.com";
    private String ownerEmail = "owner@test.com";
    private String testPassword = "password123";
    private User renterUser;
    private User ownerUser;
    private Listing testListing;

    @Given("I have selected a {string} that fits my needs")
    public void i_have_selected_a_vehicle_that_fits_my_needs(String vehicleType) {
        // Clean up database
        bookingRepository.deleteAll();
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
            .birthdate(LocalDate.of(1985, 5, 15))
            .rating(4.5)
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
            .birthdate(LocalDate.of(1995, 8, 20))
            .rating(4.8)
            .role(Role.NORMAL_USER)
            .build();
        renterUser = userRepository.save(renterUser);

        // Create listing with availability
        testListing = new Listing();
        testListing.setTitle("Test " + vehicleType.substring(0, 1).toUpperCase() + vehicleType.substring(1));
        testListing.setDescription("Perfect " + vehicleType + " for weekend trips");
        testListing.setPrice(BigDecimal.valueOf(50.00));
        testListing.setState(ListingState.AVAILABLE);
        testListing.setPickUpLocation("City Center");
        testListing.setDropOffLocation("City Center");

        Vehicle vehicle = new Vehicle();
        vehicle.setType(vehicleType.substring(0, 1).toUpperCase() + vehicleType.substring(1));
        vehicle.setCondition(VehicleCondition.EXCELLENT);
        testListing.setVehicle(vehicle);

        // Add availability periods (available on weekends for the next month)
        Set<AvailabilityPeriod> availabilityPeriods = new HashSet<>();
        
        // Add Saturday availability
        AvailabilityPeriod saturdayPeriod = new AvailabilityPeriod();
        saturdayPeriod.setStartDay(DayOfWeek.SATURDAY);
        saturdayPeriod.setEndDay(DayOfWeek.SATURDAY);
        saturdayPeriod.setStartTime(LocalTime.of(8, 0));
        saturdayPeriod.setEndTime(LocalTime.of(20, 0));
        availabilityPeriods.add(saturdayPeriod);
        
        // Add Sunday availability
        AvailabilityPeriod sundayPeriod = new AvailabilityPeriod();
        sundayPeriod.setStartDay(DayOfWeek.SUNDAY);
        sundayPeriod.setEndDay(DayOfWeek.SUNDAY);
        sundayPeriod.setStartTime(LocalTime.of(8, 0));
        sundayPeriod.setEndTime(LocalTime.of(20, 0));
        availabilityPeriods.add(sundayPeriod);
        
        testListing.setAvailability(availabilityPeriods);

        ownerUser.addListing(testListing);
        testListing = listingRepository.save(testListing);

        // Log in as renter and navigate to the listing
        driver.get(frontendUrl + "/login");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-form")));

        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-email")));
        emailField.clear();
        emailField.sendKeys(renterEmail);

        WebElement passwordField = driver.findElement(By.id("login-password"));
        passwordField.clear();
        passwordField.sendKeys(testPassword);

        WebElement submitButton = driver.findElement(By.id("login-submit-btn"));
        submitButton.click();

        // Wait for redirect to explore page
        wait.until(d -> d.getCurrentUrl().contains("/explore"));

        // Wait for listings to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("listings-grid")));
        
        // Find the specific listing card and click "Book Now" button
        WebElement listingCard = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("listing-" + testListing.getId()))
        );
        
        // Scroll to the listing if needed
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", listingCard);
        
        // Find and click the "Book Now" button within this listing card
        WebElement bookNowButton = listingCard.findElement(By.xpath(".//button[contains(text(), 'Book Now')]"));
        bookNowButton.click();
        
        // Wait for the booking modal to appear
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking-modal")));
    }

    @When("I choose the rental dates {string} and {string} on the calendar")
    public void i_choose_the_rental_dates_on_the_calendar(String startDay, String endDay) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Calculate the next Saturday and Sunday
        LocalDate today = LocalDate.now();
        LocalDate nextSaturday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate nextSunday = nextSaturday.plusDays(1);

        // Wait for the booking form to be present
        WebElement bookingForm = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("booking-form"))
        );

        // Format datetime-local values (YYYY-MM-DDTHH:MM)
        String pickupDateTime = nextSaturday.toString() + "T10:00";
        String dropoffDateTime = nextSunday.toString() + "T18:00";

        // Find and fill the pickup datetime field
        WebElement pickupDateTimeField = bookingForm.findElement(By.id("pickup-datetime"));
        pickupDateTimeField.clear();
        pickupDateTimeField.sendKeys(pickupDateTime);

        // Find and fill the dropoff datetime field
        WebElement dropoffDateTimeField = bookingForm.findElement(By.id("dropoff-datetime"));
        dropoffDateTimeField.clear();
        dropoffDateTimeField.sendKeys(dropoffDateTime);
    }

    @Then("I submit the booking request to the owner for approval")
    public void i_submit_the_booking_request_to_the_owner_for_approval() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Find and click the submit booking button
        WebElement submitButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("submit-booking-button"))
        );
        
        submitButton.click();

        // Wait for success alert/message
        wait.until(ExpectedConditions.alertIsPresent());
        String alertText = driver.switchTo().alert().getText();
        assertTrue(
            alertText.contains("Booking request submitted") || 
            alertText.contains("successfully") ||
            alertText.contains("owner will review"),
            "Success alert should indicate booking was submitted"
        );
        driver.switchTo().alert().accept();
        
        // Modal should close after successful submission
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("booking-modal")));
    }

    @And("I wait for the owner's approval")
    public void i_wait_for_the_owners_approval() {
        // Verify that the booking was created in the database with REQUESTED state
        List<Booking> bookings = bookingRepository.findByRenter(renterUser);
        
        assertNotNull(bookings, "Bookings list should not be null");
        assertTrue(bookings.size() > 0, "At least one booking should exist");
        
        Booking booking = bookings.get(0);
        
        assertEquals(BookingState.REQUESTED, booking.getState(), 
            "Booking should be in REQUESTED state waiting for owner approval");
        assertEquals(testListing.getId(), booking.getListing().getId(), 
            "Booking should be associated with the correct listing");
        assertEquals(renterUser.getId(), booking.getRenter().getId(), 
            "Booking should be associated with the correct renter");
        
        assertNotNull(booking.getPickupDateTime(), "Pickup datetime should be set");
        assertNotNull(booking.getDropoffDateTime(), "Dropoff datetime should be set");
        
        assertTrue(booking.getDropoffDateTime().isAfter(booking.getPickupDateTime()),
            "Dropoff should be after pickup");
    }
}
