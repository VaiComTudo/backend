package com.vaicomtudo.backend.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

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

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class FilterByLocationSteps {

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

    private String currentUserEmail = "renter@test.com";
    private String otherUserEmail = "owner@test.com";
    private String testPassword = "password123";
    private User currentUser;
    private User otherUser;

    @Given("I am browsing available {string}")
    public void i_am_browsing_available(String category) {
        // Clean up existing data
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create current user account (logged in renter)
        Account currentAccount = Account.builder()
            .email(currentUserEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Current Renter")
            .build();
        currentAccount = accountRepository.save(currentAccount);

        currentUser = User.builder()
            .account(currentAccount)
            .birthdate(LocalDate.of(1992, 3, 10))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        currentUser = userRepository.save(currentUser);

        // Create other user account (owner of listings)
        Account otherAccount = Account.builder()
            .email(otherUserEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Listing Owner")
            .build();
        otherAccount = accountRepository.save(otherAccount);

        otherUser = User.builder()
            .account(otherAccount)
            .birthdate(LocalDate.of(1988, 7, 20))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        otherUser = userRepository.save(otherUser);

        // Create bike listing in Aveiro
        createListing(
            otherUser,
            "Aveiro Mountain Bike",
            "Great bike for Aveiro trails",
            "25.00",
            VehicleCondition.GOOD,
            "bike",
            "Aveiro Centro",
            "Aveiro Station"
        );

        // Create another bike listing in Aveiro
        createListing(
            otherUser,
            "Aveiro City Bike",
            "Perfect for city rides in Aveiro",
            "20.00",
            VehicleCondition.EXCELLENT,
            "bike",
            "Aveiro University",
            "Aveiro Forum"
        );

        // Create bike listing in Porto (should not appear)
        createListing(
            otherUser,
            "Porto Racing Bike",
            "Fast bike in Porto",
            "30.00",
            VehicleCondition.GOOD,
            "bike",
            "Porto Downtown",
            "Porto Airport"
        );

        // Create bike listing in Lisboa (should not appear)
        createListing(
            otherUser,
            "Lisboa Touring Bike",
            "Comfortable bike in Lisboa",
            "28.00",
            VehicleCondition.EXCELLENT,
            "bike",
            "Lisboa Centro",
            "Lisboa Belém"
        );

        // Create listing by current user in Aveiro (should not appear - own listing)
        createListing(
            currentUser,
            "My Aveiro Bike",
            "My personal bike",
            "22.00",
            VehicleCondition.GOOD,
            "bike",
            "Aveiro Glicínias",
            "Aveiro Shopping"
        );

        // Login the current user
        driver.get(frontendUrl + "/login");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-form")));

        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-email")));
        emailField.clear();
        emailField.sendKeys(currentUserEmail);

        WebElement passwordField = driver.findElement(By.id("login-password"));
        passwordField.clear();
        passwordField.sendKeys(testPassword);

        WebElement submitButton = driver.findElement(By.id("login-submit-btn"));
        submitButton.click();

        // Wait for redirect to explore page
        wait.until(d -> d.getCurrentUrl().contains("/explore"));
        
        // Wait for search form to be present
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-form")));
    }

    @When("I apply the location filter and select {string}")
    public void i_apply_the_location_filter_and_select(String location) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Find and fill the location field
        WebElement locationField = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("search-location"))
        );
        locationField.clear();
        locationField.sendKeys(location);

        // Submit the search form
        WebElement searchButton = driver.findElement(By.id("search-button"));
        searchButton.click();

        // Wait for results to load
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("listings-grid")),
            ExpectedConditions.presenceOfElementLocated(By.id("no-results"))
        ));
    }

    @Then("the system displays only bicycles available in {string}")
    public void the_system_displays_only_bicycles_available_in(String location) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for listings grid
        WebElement listingsGrid = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("listings-grid"))
        );

        // Get all listing cards
        List<WebElement> listingCards = listingsGrid.findElements(By.className("listing-card"));

        // Should have exactly 2 listings (both Aveiro bikes from other user, excluding own listing)
        assertTrue(listingCards.size() >= 2, 
            "Expected at least 2 bicycle listings in " + location + ", but found " + listingCards.size());

        // Verify each displayed listing is from Aveiro and is a bike
        int aveiroCount = 0;
        for (WebElement card : listingCards) {
            WebElement titleElement = card.findElement(By.className("listing-title"));
            String title = titleElement.getText();

            // Verify it's not the user's own listing
            assertFalse(title.equals("My Aveiro Bike"), 
                "Own listing should not be displayed in search results");

            // Verify vehicle type is bike
            WebElement vehicleElement = card.findElement(By.className("listing-vehicle"));
            String vehicleText = vehicleElement.getText().toLowerCase();
            assertTrue(vehicleText.contains("bike") || vehicleText.contains("bicycle"), 
                "Vehicle type should be bike");

            // Verify location contains Aveiro
            List<WebElement> locationElements = card.findElements(By.className("listing-location"));
            boolean hasAveiroLocation = false;
            for (WebElement locElement : locationElements) {
                String locText = locElement.getText();
                if (locText.contains(location)) {
                    hasAveiroLocation = true;
                    aveiroCount++;
                    break;
                }
            }

            assertTrue(hasAveiroLocation, 
                "Listing '" + title + "' should have " + location + " in its location");

            // Verify all required information is displayed
            WebElement descriptionElement = card.findElement(By.className("listing-description"));
            assertTrue(descriptionElement.isDisplayed(), "Description should be displayed");

            WebElement priceElement = card.findElement(By.className("listing-price"));
            assertTrue(priceElement.isDisplayed(), "Price should be displayed");
            assertTrue(priceElement.getText().contains("€"), "Price should contain currency symbol");
        }

        // Verify we found the correct number of Aveiro listings
        assertTrue(aveiroCount >= 2, 
            "Should find at least 2 bicycle listings in " + location + " (excluding own listings)");
    }

    @Then("items from other locations are excluded from the results")
    public void items_from_other_locations_are_excluded_from_the_results() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Get all listing cards
        WebElement listingsGrid = driver.findElement(By.id("listings-grid"));
        List<WebElement> listingCards = listingsGrid.findElements(By.className("listing-card"));

        // Verify no Porto or Lisboa listings are shown
        for (WebElement card : listingCards) {
            WebElement titleElement = card.findElement(By.className("listing-title"));
            String title = titleElement.getText().toLowerCase();

            assertFalse(title.contains("porto"), 
                "Porto listings should not be visible when filtering by Aveiro");
            assertFalse(title.contains("lisboa"), 
                "Lisboa listings should not be visible when filtering by Aveiro");

            // Verify location doesn't contain Porto or Lisboa
            List<WebElement> locationElements = card.findElements(By.className("listing-location"));
            for (WebElement locElement : locationElements) {
                String locText = locElement.getText().toLowerCase();
                assertFalse(locText.contains("porto") && !locText.contains("aveiro"), 
                    "Should not show listings with Porto location only");
                assertFalse(locText.contains("lisboa") && !locText.contains("aveiro"), 
                    "Should not show listings with Lisboa location only");
            }
        }

        // Verify results summary
        WebElement resultsSummary = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("results-summary"))
        );
        String summaryText = resultsSummary.getText();

        assertTrue(summaryText.matches("Found \\d+ listing(s)?"), 
            "Results summary should show listing count");
        
        // Extract count from summary
        String[] parts = summaryText.split(" ");
        if (parts.length >= 2) {
            int count = Integer.parseInt(parts[1]);
            assertTrue(count >= 2, "Should show at least 2 listings in Aveiro");
        }
    }

    private void createListing(
        User owner,
        String title,
        String description,
        String price,
        VehicleCondition condition,
        String vehicleType,
        String pickUpLocation,
        String dropOffLocation
    ) {
        Vehicle vehicle = new Vehicle();
        vehicle.setType(vehicleType);
        vehicle.setCondition(condition);

        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setPrice(new BigDecimal(price));
        listing.setState(ListingState.AVAILABLE);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation(pickUpLocation);
        listing.setDropOffLocation(dropOffLocation);

        listingRepository.save(listing);
    }
}
