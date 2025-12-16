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
import com.vaicomtudo.backend.data.repository.BookingRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class SearchListingsSteps {

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

    @Autowired
    private BookingRepository bookingRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String currentUserEmail = "currentuser@test.com";
    private String otherUserEmail = "otheruser@test.com";
    private String testPassword = "password123";
    private User currentUser;
    private User otherUser;
    private String ownListingTitle = "My Own Bicycle";
    private String otherListingTitle = "Other User's Bicycle";

    @Given("I want to find a bike for a weekend leisure ride")
    public void i_want_to_find_a_bike_for_a_weekend_leisure_ride() {
        // Clean up existing data
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create current user account
        Account currentAccount = Account.builder()
            .email(currentUserEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Current User")
            .build();
        currentAccount = accountRepository.save(currentAccount);

        currentUser = User.builder()
            .account(currentAccount)
            .birthdate(LocalDate.of(1990, 1, 1))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        currentUser = userRepository.save(currentUser);

        // Create other user account
        Account otherAccount = Account.builder()
            .email(otherUserEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Other User")
            .build();
        otherAccount = accountRepository.save(otherAccount);

        otherUser = User.builder()
            .account(otherAccount)
            .birthdate(LocalDate.of(1995, 5, 15))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();
        otherUser = userRepository.save(otherUser);

        // Create listing owned by current user (should not appear in search)
        createListing(
            currentUser,
            ownListingTitle,
            "My personal mountain bike",
            "25.00",
            VehicleCondition.GOOD,
            "bike",
            "Aveiro Centro",
            "Aveiro Station"
        );

        // Create listing owned by other user (should appear in search)
        createListing(
            otherUser,
            otherListingTitle,
            "Great bike for trails",
            "30.00",
            VehicleCondition.EXCELLENT,
            "bike",
            "Porto Downtown",
            "Porto Airport"
        );

        // Create scooter listing by other user (should not appear when filtering by bike)
        createListing(
            otherUser,
            "Electric Scooter",
            "Fast electric scooter",
            "20.00",
            VehicleCondition.GOOD,
            "scooter",
            "Lisboa Centro",
            "Lisboa Airport"
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
    }

    @When("I select the category filter and choose {string}")
    public void i_select_the_category_filter_and_choose(String category) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for the search form to be present
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-form")));

        // Find and fill the category field
        WebElement categoryField = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("search-category"))
        );
        categoryField.clear();
        categoryField.sendKeys(category);

        // Submit the search form
        WebElement searchButton = driver.findElement(By.id("search-button"));
        searchButton.click();

        // Wait for results to load
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("listings-grid")),
            ExpectedConditions.presenceOfElementLocated(By.id("no-results"))
        ));
    }

    @Then("the system displays all available bicycles with specifications")
    public void the_system_displays_all_available_bicycles_with_specifications() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for listings grid
        WebElement listingsGrid = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("listings-grid"))
        );

        // Get all listing cards
        List<WebElement> listingCards = listingsGrid.findElements(By.className("listing-card"));

        // Should have at least 1 listing (the other user's bike)
        assertTrue(listingCards.size() >= 1, 
            "Expected at least 1 bicycle listing, but found " + listingCards.size());

        // Verify the displayed listing is the other user's bike
        boolean foundOtherUserBike = false;
        for (WebElement card : listingCards) {
            WebElement titleElement = card.findElement(By.className("listing-title"));
            String title = titleElement.getText();
            
            if (title.equals(otherListingTitle)) {
                foundOtherUserBike = true;
                
                // Verify it has all required information
                WebElement descriptionElement = card.findElement(By.className("listing-description"));
                assertTrue(descriptionElement.isDisplayed(), "Description should be displayed");
                
                WebElement priceElement = card.findElement(By.className("listing-price"));
                assertTrue(priceElement.isDisplayed(), "Price should be displayed");
                assertTrue(priceElement.getText().contains("€"), "Price should contain currency symbol");
                
                WebElement vehicleElement = card.findElement(By.className("listing-vehicle"));
                assertTrue(vehicleElement.isDisplayed(), "Vehicle info should be displayed");
                assertTrue(vehicleElement.getText().toLowerCase().contains("bike"), 
                    "Vehicle type should be bike");
                
                WebElement locationElement = card.findElement(By.className("listing-location"));
                assertTrue(locationElement.isDisplayed(), "Location should be displayed");
            }
            
            // Verify that own listing is NOT displayed
            if (title.equals(ownListingTitle)) {
                throw new AssertionError("Own listing '" + ownListingTitle + "' should not be displayed in search results");
            }
        }

        assertTrue(foundOtherUserBike, 
            "Should find the other user's bicycle listing in search results");
    }

    @Then("items from other categories are hidden from the results")
    public void items_from_other_categories_are_hidden_from_the_results() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Get all listing cards
        WebElement listingsGrid = driver.findElement(By.id("listings-grid"));
        List<WebElement> listingCards = listingsGrid.findElements(By.className("listing-card"));

        // Verify no scooter listings are shown
        for (WebElement card : listingCards) {
            WebElement vehicleElement = card.findElement(By.className("listing-vehicle"));
            String vehicleText = vehicleElement.getText().toLowerCase();
            
            assertFalse(vehicleText.contains("scooter"), 
                "Scooter listings should not be visible when filtering by bike category");
            
            assertTrue(vehicleText.contains("bike") || vehicleText.contains("bicycle"), 
                "Only bike/bicycle listings should be visible");
        }

        // Verify results summary shows correct count
        WebElement resultsSummary = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("results-summary"))
        );
        String summaryText = resultsSummary.getText();
        
        // Should show at least 1 listing (other user's bike, but not own bike)
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
