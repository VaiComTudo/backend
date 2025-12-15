package com.vaicomtudo.backend.cucumber.steps;

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
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.BookingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CreateListingSteps {

    @Autowired
    private WebDriver driver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String testEmail = "email@email.com";
    private String testPassword = "pass123";

    public void registerTestAccount() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();

        Account account = Account.builder()
            .email(testEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Test User")
            .build();
        
        account = accountRepository.save(account);

        User user = User.builder()
            .account(account)
            .birthdate(LocalDate.of(2000, 1, 1))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();

        userRepository.save(user);
    }

    @Given("I am logged in")
    public void i_am_logged_in() {
        registerTestAccount();

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

        wait.until(d -> {
            return d.getCurrentUrl().contains("/explore");
        });
    }

    @When("I click the {string} button")
    public void I_click_the_button(String buttonLabel) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for and click the "Add New Listing" button
        WebElement addListingButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), '" + buttonLabel + "')]"))
        );
        addListingButton.click();
        
        // Wait for modal to appear
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//h2[contains(text(), 'Create New Listing')]")));
    }
    
    @When("Fill in the form with the title {string}, the description {string}, a daily price of {string} euros, a {string} {string} vehicle to be picked up and dropped off at {string}.")
    public void Fill_in_the_form(String title, String description, String price, String condition, String vehicleType, String meetingLocation) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Fill in Title
        WebElement titleInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("title")));
        titleInput.clear();
        titleInput.sendKeys(title);
        
        // Fill in Description
        WebElement descriptionInput = driver.findElement(By.name("description"));
        descriptionInput.clear();
        descriptionInput.sendKeys(description);
        
        // Fill in Price
        WebElement priceInput = driver.findElement(By.name("price"));
        priceInput.clear();
        priceInput.sendKeys(price);
        
        // Select Vehicle Condition
        WebElement conditionSelect = driver.findElement(By.name("vehicleCondition"));
        conditionSelect.sendKeys(condition);
        
        // Fill in Vehicle Type
        WebElement vehicleTypeInput = driver.findElement(By.name("vehicleType"));
        vehicleTypeInput.clear();
        vehicleTypeInput.sendKeys(vehicleType);
        
        // Fill in Pick-up Location
        WebElement pickUpInput = driver.findElement(By.name("pickUpLocation"));
        pickUpInput.clear();
        pickUpInput.sendKeys(meetingLocation);
        
        // Fill in Drop-off Location
        WebElement dropOffInput = driver.findElement(By.name("dropOffLocation"));
        dropOffInput.clear();
        dropOffInput.sendKeys(meetingLocation);
        
        // Submit the form
        WebElement submitButton = driver.findElement(By.xpath("//button[@type='submit' and contains(text(), 'Create Listing')]"));
        submitButton.click();
    }
    
    @Then("The system creates the listing")
    public void The_system_creates_the_listing() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for success message toast to appear
        WebElement successToast = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Listing created successfully!')]")
            )
        );
        
        // Verify success message is displayed
        assert successToast.isDisplayed() : "Success message should be displayed";
        
        // Wait for modal to close (up to 3 seconds as per frontend code)
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.xpath("//h2[contains(text(), 'Create New Listing')]")
        ));
    }
}