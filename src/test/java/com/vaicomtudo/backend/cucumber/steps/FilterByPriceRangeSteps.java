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

public class FilterByPriceRangeSteps {

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
    private String owner1Email = "owner1@test.com";
    private String owner2Email = "owner2@test.com";
    private String testPassword = "password123";
    private User currentUser;
    private User owner1;
    private User owner2;

    @Given("I am browsing available bicycles in {string}")
    public void i_am_browsing_available_bicycles_in(String location) {
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
            .birthdate(LocalDate.of(1993, 6, 15))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        currentUser = userRepository.save(currentUser);

        // Create first owner account
        Account owner1Account = Account.builder()
            .email(owner1Email)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Bike Owner 1")
            .build();
        owner1Account = accountRepository.save(owner1Account);

        owner1 = User.builder()
            .account(owner1Account)
            .birthdate(LocalDate.of(1985, 4, 10))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        owner1 = userRepository.save(owner1);

        // Create second owner account
        Account owner2Account = Account.builder()
            .email(owner2Email)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Bike Owner 2")
            .build();
        owner2Account = accountRepository.save(owner2Account);

        owner2 = User.builder()
            .account(owner2Account)
            .birthdate(LocalDate.of(1990, 9, 25))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        owner2 = userRepository.save(owner2);

        // Create affordable bikes in Aveiro (should appear - price <= 20)
        createListing(
            owner1,
            "Budget City Bike",
            "Affordable bike for city rides",
            "15.00",
            VehicleCondition.GOOD,
            "bike",
            "Aveiro Centro",
            "Aveiro Station"
        );

        createListing(
            owner1,
            "Economy Mountain Bike",
            "Great value mountain bike",
            "18.50",
            VehicleCondition.GOOD,
            "bike",
            "Aveiro University",
            "Aveiro Forum"
        );

        createListing(
            owner2,
            "Standard Bike",
            "Exactly at budget limit",
            "20.00",
            VehicleCondition.EXCELLENT,
            "bike",
            "Aveiro Glicínias",
            "Aveiro Shopping"
        );

        // Create expensive bikes in Aveiro (should NOT appear - price > 20)
        createListing(
            owner1,
            "Premium Racing Bike",
            "High-end racing bike",
            "35.00",
            VehicleCondition.EXCELLENT,
            "bike",
            "Aveiro Centro",
            "Aveiro Port"
        );

        createListing(
            owner2,
            "Luxury Touring Bike",
            "Top quality touring bike",
            "50.00",
            VehicleCondition.EXCELLENT,
            "bike",
            "Aveiro Downtown",
            "Aveiro Beach"
        );

        // Create affordable bike by current user in Aveiro (should NOT appear - own listing)
        createListing(
            currentUser,
            "My Budget Bike",
            "My personal cheap bike",
            "12.00",
            VehicleCondition.GOOD,
            "bike",
            "Aveiro My Place",
            "Aveiro Work"
        );

        // Create affordable bike in Porto (should NOT appear - wrong location)
        createListing(
            owner1,
            "Porto Cheap Bike",
            "Affordable bike in Porto",
            "10.00",
            VehicleCondition.GOOD,
            "bike",
            "Porto Downtown",
            "Porto Airport"
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

    @When("I set the price range filter to up to {string} euros\\/day")
    public void i_set_the_price_range_filter_to_up_to_euros_day(String maxPrice) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Find and fill the max price field
        WebElement maxPriceField = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("search-max-price"))
        );
        maxPriceField.clear();
        maxPriceField.sendKeys(maxPrice);

        // Optionally set min price to 0 for clarity
        WebElement minPriceField = driver.findElement(By.id("search-min-price"));
        minPriceField.clear();
        minPriceField.sendKeys("0");

        // Submit the search form
        WebElement searchButton = driver.findElement(By.id("search-button"));
        searchButton.click();

        // Wait for results to load
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("listings-grid")),
            ExpectedConditions.presenceOfElementLocated(By.id("no-results"))
        ));
    }

    @Then("the system displays only bicycles priced at {string} euros\\/day or less")
    public void the_system_displays_only_bicycles_priced_at_euros_day_or_less(String maxPrice) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for listings grid
        WebElement listingsGrid = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("listings-grid"))
        );

        // Get all listing cards
        List<WebElement> listingCards = listingsGrid.findElements(By.className("listing-card"));

        // Should have exactly 3 listings (affordable bikes from other users, excluding own listing)
        assertTrue(listingCards.size() >= 3, 
            "Expected at least 3 bicycle listings priced at " + maxPrice + " or less, but found " + listingCards.size());

        double maxPriceValue = Double.parseDouble(maxPrice);
        int affordableCount = 0;

        // Verify each displayed listing is within price range
        for (WebElement card : listingCards) {
            WebElement titleElement = card.findElement(By.className("listing-title"));
            String title = titleElement.getText();

            // Verify it's not the user's own listing
            assertFalse(title.equals("My Budget Bike"), 
                "Own listing should not be displayed in search results");

            // Verify it's not an expensive bike
            assertFalse(title.contains("Premium") || title.contains("Luxury"), 
                "Expensive bikes should not be displayed");

            // Verify price is within range
            WebElement priceElement = card.findElement(By.className("listing-price"));
            String priceText = priceElement.getText();
            
            // Extract numeric price (remove €, whitespace, etc.)
            String numericPrice = priceText.replaceAll("[^0-9.]", "");
            double price = Double.parseDouble(numericPrice);

            assertTrue(price <= maxPriceValue, 
                "Listing '" + title + "' has price " + price + " which exceeds max price " + maxPriceValue);

            affordableCount++;

            // Verify vehicle type is bike
            WebElement vehicleElement = card.findElement(By.className("listing-vehicle"));
            String vehicleText = vehicleElement.getText().toLowerCase();
            assertTrue(vehicleText.contains("bike") || vehicleText.contains("bicycle"), 
                "Vehicle type should be bike");
        }

        assertTrue(affordableCount >= 3, 
            "Should find at least 3 affordable bicycle listings (excluding own listings)");
    }

    @Then("items exceeding my price range are excluded from the results")
    public void items_exceeding_my_price_range_are_excluded_from_the_results() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Get all listing cards
        WebElement listingsGrid = driver.findElement(By.id("listings-grid"));
        List<WebElement> listingCards = listingsGrid.findElements(By.className("listing-card"));

        // Verify no expensive bikes are shown
        for (WebElement card : listingCards) {
            WebElement titleElement = card.findElement(By.className("listing-title"));
            String title = titleElement.getText();

            assertFalse(title.contains("Premium"), 
                "Premium bikes should not be visible when filtering by price range");
            assertFalse(title.contains("Luxury"), 
                "Luxury bikes should not be visible when filtering by price range");

            // Verify price doesn't exceed 20
            WebElement priceElement = card.findElement(By.className("listing-price"));
            String priceText = priceElement.getText();
            String numericPrice = priceText.replaceAll("[^0-9.]", "");
            double price = Double.parseDouble(numericPrice);

            assertTrue(price <= 20.0, 
                "Price " + price + " exceeds maximum of 20");
        }
    }

    @Then("the daily price is clearly displayed for each item")
    public void the_daily_price_is_clearly_displayed_for_each_item() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Get all listing cards
        WebElement listingsGrid = driver.findElement(By.id("listings-grid"));
        List<WebElement> listingCards = listingsGrid.findElements(By.className("listing-card"));

        assertTrue(listingCards.size() > 0, "Should have at least one listing to verify price display");

        // Verify each listing has a clearly displayed price
        for (WebElement card : listingCards) {
            WebElement priceElement = card.findElement(By.className("listing-price"));
            
            assertTrue(priceElement.isDisplayed(), 
                "Price should be visible");

            String priceText = priceElement.getText();
            
            assertTrue(priceText.contains("€"), 
                "Price should contain currency symbol (€)");

            // Verify price text is not empty
            assertFalse(priceText.trim().isEmpty(), 
                "Price text should not be empty");

            // Verify we can extract a numeric value
            String numericPrice = priceText.replaceAll("[^0-9.]", "");
            assertTrue(numericPrice.length() > 0, 
                "Should be able to extract numeric price value");

            double price = Double.parseDouble(numericPrice);
            assertTrue(price > 0, 
                "Price should be greater than 0");
        }

        // Verify results summary
        WebElement resultsSummary = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("results-summary"))
        );
        String summaryText = resultsSummary.getText();

        assertTrue(summaryText.matches("Found \\d+ listing(s)?"), 
            "Results summary should show listing count");
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
