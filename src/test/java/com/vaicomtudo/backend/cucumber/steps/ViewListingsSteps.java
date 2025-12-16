package com.vaicomtudo.backend.cucumber.steps;

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

public class ViewListingsSteps {

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

    private String testEmail = "owner@test.com";
    private String testPassword = "ns873vbf98";
    private User testUser;

    @Given("I am logged into my account")
    public void i_am_logged_into_my_account() {
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

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

        createTestListing(
            "Mountain Bike",
            "A great bike for trails",
            "25.00",
            VehicleCondition.GOOD,
            "bike",
            ListingState.AVAILABLE);

        createTestListing(
            "City Scooter",
            "Electric scooter for city commuting",
            "15.50",
            VehicleCondition.POOR,
            "scooter",
            ListingState.BOOKED);

        createTestListing(
            "Skateboard",
            "Pro skateboard for tricks",
            "10.00",
            VehicleCondition.GOOD,
            "skateboard",
            ListingState.UNAVAILABLE);

        createTestListing(
            "Road Bike",
            "Fast road bike",
            "30.00",
            VehicleCondition.GOOD,
            "bike",
            ListingState.MAINTENANCE);

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

        wait.until(d -> d.getCurrentUrl().contains("/explore"));
    }

    // @When("I navigate to my listings dashboard")
    // public void i_navigate_to_my_listings_dashboard() {
    //     WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    //     WebElement myListingsButton = wait.until(
    //         ExpectedConditions.elementToBeClickable(By.id("nav-my-listings"))
    //     );
    //     myListingsButton.click();

    //     wait.until(ExpectedConditions.presenceOfElementLocated(By.id("listings-container")));
    // }

    // @Then("the system displays all my listed items")
    // public void the_system_displays_all_my_listed_items() {
    //     WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    //     WebElement listingsContainer = wait.until(
    //         ExpectedConditions.presenceOfElementLocated(By.id("listings-container"))
    //     );

    //     List<WebElement> listingElements = listingsContainer.findElements(
    //         By.cssSelector("[data-testid^='listing-']")
    //     );

    //     assert listingElements.size() == 4 :
    //         String.format("Expected 4 listings but found %d", listingElements.size());
    // }

    @Then("I can see the title, description, price, and condition")
    public void i_can_see_the_title_description_price_and_condition() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        List<Listing> dbListings = listingRepository.findAll();

        for (Listing listing : dbListings) {
            // Find the listing element by ID
            String listingId = listing.getId().toString();

            // Verify title is displayed
            WebElement titleElement = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.id("listing-title-" + listingId)
                )
            );
            assert titleElement.getText().equals(listing.getTitle()) :
                String.format("Expected title '%s' but got '%s'",
                    listing.getTitle(), titleElement.getText());

            // Verify description is displayed
            WebElement descriptionElement = driver.findElement(
                By.id("listing-description-" + listingId)
            );
            assert descriptionElement.getText().equals(listing.getDescription()) :
                String.format("Expected description '%s' but got '%s'",
                    listing.getDescription(), descriptionElement.getText());

            // Verify price is displayed
            WebElement priceElement = driver.findElement(
                By.id("listing-price-" + listingId)
            );
            String actualPrice = priceElement.getText();
            String priceValue = listing.getPrice().stripTrailingZeros().toPlainString();
            String expectedPrice = "€" + priceValue;
            assert actualPrice.equals(expectedPrice) :
                String.format("Expected price '%s' but got '%s'",
                    expectedPrice, actualPrice);

            // Verify vehicle type and condition are displayed
            WebElement vehicleElement = driver.findElement(
                By.id("listing-vehicle-" + listingId)
            );
            String expectedVehicle = "Vehicle: " + listing.getVehicle().getType() + " - " +
                                    listing.getVehicle().getCondition();
            assert vehicleElement.getText().equals(expectedVehicle) :
                String.format("Expected vehicle '%s' but got '%s'",
                    expectedVehicle, vehicleElement.getText());
        }
    }

    @Then("I can see the item state")
    public void i_can_see_the_item_state() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Get all listings from the database
        List<Listing> dbListings = listingRepository.findAll();

        for (Listing listing : dbListings) {
            String listingId = listing.getId().toString();

            // Verify state is displayed
            WebElement stateElement = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.id("listing-state-" + listingId)
                )
            );

            String expectedState = listing.getState().toString();
            assert stateElement.getText().equals(expectedState) :
                String.format("Expected state '%s' but got '%s'",
                    expectedState, stateElement.getText());
        }

        List<ListingState> states = dbListings.stream()
            .map(Listing::getState)
            .distinct()
            .toList();

        assert states.contains(ListingState.AVAILABLE) : "Should have at least one AVAILABLE listing";
        assert states.contains(ListingState.BOOKED) : "Should have at least one BOOKED listing";
        assert states.contains(ListingState.UNAVAILABLE) : "Should have at least one UNAVAILABLE listing";
        assert states.contains(ListingState.MAINTENANCE) : "Should have at least one MAINTENANCE listing";
    }

    private void createTestListing(
        String title,
        String description,
        String price,
        VehicleCondition condition,
        String vehicleType,
        ListingState state
    ) {
        Vehicle vehicle = new Vehicle();
        vehicle.setType(vehicleType);
        vehicle.setCondition(condition);

        Listing listing = new Listing();
        listing.setOwner(testUser);
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setPrice(new BigDecimal(price));
        listing.setState(state);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Test Location");
        listing.setDropOffLocation("Test Location");

        listingRepository.save(listing);
    }
}
