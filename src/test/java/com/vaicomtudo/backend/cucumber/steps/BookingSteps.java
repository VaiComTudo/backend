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

        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        emailField.sendKeys(renterEmail);

        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.sendKeys(testPassword);

        WebElement loginButton = driver.findElement(By.id("login-button"));
        loginButton.click();

        // Navigate to the listing details page
        wait.until(ExpectedConditions.urlContains("/"));
        driver.get(frontendUrl + "/listings/" + testListing.getId());
        
        // Wait for listing details to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("listing-title")));
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

        // Find and fill the pickup date/time fields
        WebElement pickupDateField = bookingForm.findElement(By.id("pickup-date"));
        WebElement pickupTimeField = bookingForm.findElement(By.id("pickup-time"));
        
        // Clear and set pickup date
        pickupDateField.clear();
        pickupDateField.sendKeys(nextSaturday.toString());
        
        // Clear and set pickup time
        pickupTimeField.clear();
        pickupTimeField.sendKeys("10:00");

        // Find and fill the dropoff date/time fields
        WebElement dropoffDateField = bookingForm.findElement(By.id("dropoff-date"));
        WebElement dropoffTimeField = bookingForm.findElement(By.id("dropoff-time"));
        
        // Clear and set dropoff date
        dropoffDateField.clear();
        dropoffDateField.sendKeys(nextSunday.toString());
        
        // Clear and set dropoff time
        dropoffTimeField.clear();
        dropoffTimeField.sendKeys("18:00");
    }

    @Then("I submit the booking request to the owner for approval")
    public void i_submit_the_booking_request_to_the_owner_for_approval() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Find and click the submit booking button
        WebElement submitButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("submit-booking-button"))
        );
        
        // Use JavaScript click to avoid any overlay issues
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);

        // Wait for success message
        WebElement successMessage = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Booking request submitted') or contains(text(), 'booking submitted') or contains(text(), 'Request sent')]")
            )
        );
        
        assertTrue(successMessage.isDisplayed(), "Booking success message should be displayed");
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
