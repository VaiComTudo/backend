package com.vaicomtudo.backend.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class BrowseItemsSteps {

    @Autowired
    private WebDriver driver;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${frontend.url}")
    private String frontendUrl;

    private User owner;
    private String selectedLocation;
    private String testEmail = "renter@test.com";
    private String testPassword = "password123";

    @Given("I am browsing available bicycles")
    public void i_am_browsing_available_bicycles() {
        // Clean database
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create account and owner (for listings)
        Account ownerAccount = new Account();
        ownerAccount.setName("Test Owner");
        ownerAccount.setEmail("owner@test.com");
        ownerAccount.setPasswordHash(passwordEncoder.encode("password123"));

        owner = new User();
        owner.setAccount(ownerAccount);
        owner.setBirthdate(LocalDate.of(1990, 1, 1));
        owner.setRole(Role.NORMAL_USER);
        owner.setRating(0.0);
        owner = userRepository.save(owner);

        // Create account for renter (to browse listings)
        Account renterAccount = new Account();
        renterAccount.setName("Test Renter");
        renterAccount.setEmail(testEmail);
        renterAccount.setPasswordHash(passwordEncoder.encode(testPassword));

        User renter = new User();
        renter.setAccount(renterAccount);
        renter.setBirthdate(LocalDate.of(1995, 1, 1));
        renter.setRole(Role.NORMAL_USER);
        renter.setRating(0.0);
        userRepository.save(renter);

        // Create bicycle listings in different locations
        // Bicycle in Aveiro
        Listing aveiroBicycle = new Listing();
        aveiroBicycle.setOwner(owner);
        aveiroBicycle.setTitle("Bicycle in Aveiro");
        aveiroBicycle.setDescription("Great bicycle available in Aveiro");
        aveiroBicycle.setPrice(BigDecimal.valueOf(25.00));
        aveiroBicycle.setState(ListingState.AVAILABLE);

        Vehicle aveiroVehicle = new Vehicle();
        aveiroVehicle.setType("Bicycle");
        aveiroVehicle.setCondition(VehicleCondition.GOOD);
        aveiroBicycle.setVehicle(aveiroVehicle);
        aveiroBicycle.setPickUpLocation("Aveiro");
        aveiroBicycle.setDropOffLocation("Aveiro");
        listingRepository.save(aveiroBicycle);

        // Bicycle in Lisboa (different location)
        Listing lisboaBicycle = new Listing();
        lisboaBicycle.setOwner(owner);
        lisboaBicycle.setTitle("Bicycle in Lisboa");
        lisboaBicycle.setDescription("Great bicycle available in Lisboa");
        lisboaBicycle.setPrice(BigDecimal.valueOf(30.00));
        lisboaBicycle.setState(ListingState.AVAILABLE);

        Vehicle lisboaVehicle = new Vehicle();
        lisboaVehicle.setType("Bicycle");
        lisboaVehicle.setCondition(VehicleCondition.EXCELLENT);
        lisboaBicycle.setVehicle(lisboaVehicle);
        lisboaBicycle.setPickUpLocation("Lisboa");
        lisboaBicycle.setDropOffLocation("Lisboa");
        listingRepository.save(lisboaBicycle);

        // Login first (required to view listings) - following pattern from
        // CreateListingSteps
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

        // Wait for redirect to explore page or error - following pattern from
        // LoginSteps
        wait.until(d -> {
            String currentUrl = d.getCurrentUrl();
            boolean isRedirected = currentUrl.contains("/explore");
            boolean hasError = !d.findElements(By.id("login-error")).isEmpty();
            return isRedirected || hasError;
        });

        // Verify login was successful (no error)
        boolean hasError = !driver.findElements(By.id("login-error")).isEmpty();
        if (hasError) {
            throw new RuntimeException("Login failed - error message present on page");
        }

        // Wait for page to load - wait for the location filter input (indicates page is
        // loaded)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@placeholder='Digite uma localização (ex: Lisboa, Porto...)']")));

        // Wait a bit for listings to start loading
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Wait for loading to finish or listings to appear
        // The loading text is "Carregando listings..." in the frontend
        // The empty state text is "Nenhum listing encontrado." in the frontend
        // Wait for either listings to appear (h3 with titles) or empty state
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//h3[contains(text(), 'Bicycle')]")),
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//p[contains(text(), 'Nenhum listing encontrado')]"))));
    }

    @When("I apply the location filter and select {string}")
    public void i_apply_the_location_filter_and_select(String location) {
        this.selectedLocation = location;

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Find and fill the location input
        WebElement locationInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@placeholder='Digite uma localização (ex: Lisboa, Porto...)']")));
        locationInput.clear();

        // Small delay to ensure clear is processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        locationInput.sendKeys(location);

        // Wait for the input value to be updated (React state update)
        // Use both getDomProperty and getDomAttribute as fallback
        wait.until(d -> {
            try {
                WebElement input = d.findElement(
                        By.xpath("//input[@placeholder='Digite uma localização (ex: Lisboa, Porto...)']"));
                String value = input.getDomProperty("value");
                if (value == null || value.isEmpty()) {
                    value = input.getDomAttribute("value");
                }
                return location.equals(value);
            } catch (Exception e) {
                return false;
            }
        });

        // Additional delay to ensure React state is fully updated and onChange handler
        // has fired
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify the input still has the correct value before clicking
        WebElement verifyInput = driver.findElement(
                By.xpath("//input[@placeholder='Digite uma localização (ex: Lisboa, Porto...)']"));
        String verifyValue = verifyInput.getDomProperty("value");
        if (verifyValue == null || verifyValue.isEmpty()) {
            verifyValue = verifyInput.getDomAttribute("value");
        }
        if (!location.equals(verifyValue)) {
            throw new RuntimeException("Input value mismatch. Expected: " + location + ", Got: " + verifyValue);
        }

        // Click the search button
        WebElement searchButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(), 'Buscar')]")));
        searchButton.click();

        // Wait for loading to start (optional - may not always appear)
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//p[contains(text(), 'Carregando listings...')]")),
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//h3[contains(text(), 'Bicycle')]")),
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//p[contains(text(), 'Nenhum listing encontrado')]"))));
        } catch (Exception e) {
            // Continue if loading doesn't appear
        }

        // Wait for loading to finish - wait for loading text to disappear
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath("//p[contains(text(), 'Carregando listings...')]")));
        } catch (Exception e) {
            // Loading text may not appear or may have already disappeared, continue
        }

        // Wait for listings to update - wait for results to appear
        // Wait for either filtered listings to appear or empty state
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//h3[contains(text(), 'Bicycle')]")),
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//p[contains(text(), 'Nenhum listing encontrado')]"))));

        // Check for error messages
        java.util.List<WebElement> errorElements = driver.findElements(
                By.xpath("//div[contains(@style, 'f8d7da') or contains(@style, '721c24')]"));
        if (!errorElements.isEmpty()) {
            String errorText = errorElements.get(0).getText();
            throw new RuntimeException("Error displayed on page after filtering: " + errorText);
        }

        // Additional wait to ensure DOM is fully updated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Then("the system displays only bicycles available in {string}")
    public void the_system_displays_only_bicycles_available_in(String location) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // First check if there are any listings at all
        java.util.List<WebElement> allListings = driver.findElements(
                By.xpath("//h3[contains(text(), 'Bicycle')]"));

        if (allListings.isEmpty()) {
            // Check if empty state is shown
            java.util.List<WebElement> emptyState = driver.findElements(
                    By.xpath("//p[contains(text(), 'Nenhum listing encontrado')]"));
            if (!emptyState.isEmpty()) {
                // Check what the current URL is and what the input value is for debugging
                String currentUrl = driver.getCurrentUrl();
                WebElement locationInput = driver.findElement(
                        By.xpath("//input[@placeholder='Digite uma localização (ex: Lisboa, Porto...)']"));
                String inputValue = locationInput.getDomProperty("value");
                if (inputValue == null || inputValue.isEmpty()) {
                    inputValue = locationInput.getDomAttribute("value");
                }
                throw new AssertionError("No listings found after filtering by location: " + location
                        + ". Current URL: " + currentUrl
                        + ". Input value: " + inputValue);
            }
            throw new AssertionError("No bicycle listings found on page after filtering");
        }

        // Wait for the specific bicycle listing to appear
        WebElement locationBicycle = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//h3[contains(text(), 'Bicycle in " + location + "')]")));
        assertThat(locationBicycle).isNotNull();
        assertThat(locationBicycle.isDisplayed()).isTrue();
    }

    @And("items from other locations are excluded from the result")
    public void items_from_other_locations_are_excluded_from_the_result() {
        // Verify that bicycles from other locations are NOT displayed
        // Since we created bicycles in "Aveiro" and "Lisboa", if "Aveiro" is selected,
        // "Lisboa" should not be displayed, and vice versa
        if ("Aveiro".equals(selectedLocation)) {
            assertThat(driver.findElements(By.xpath("//h3[contains(text(), 'Bicycle in Lisboa')]"))).isEmpty();
        } else if ("Lisboa".equals(selectedLocation)) {
            assertThat(driver.findElements(By.xpath("//h3[contains(text(), 'Bicycle in Aveiro')]"))).isEmpty();
        }
    }
}
