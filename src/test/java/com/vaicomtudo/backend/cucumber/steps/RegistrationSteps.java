package com.vaicomtudo.backend.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.vaicomtudo.backend.data.repository.BookingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;

public class RegistrationSteps {

    @Autowired
    private WebDriver driver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String testEmail;

    @Given("I am a new user visiting the platform")
    public void i_am_a_new_user_visiting_the_platform() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @When("I navigate to the registration page")
    public void i_navigate_to_the_registration_page() {
        driver.get(frontendUrl + "/register");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("register-form")));
    }

    @When("I provide my email {string}, password {string}, name {string}, and birthdate {string}")
    public void i_provide_my_details(String email, String password, String name, String birthdate) {
        this.testEmail = email;

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement nameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("register-name")));
        nameField.clear();
        nameField.sendKeys(name);

        WebElement emailField = driver.findElement(By.id("register-email"));
        emailField.clear();
        emailField.sendKeys(email);

        WebElement passwordField = driver.findElement(By.id("register-password"));
        passwordField.clear();
        passwordField.sendKeys(password);

        WebElement birthdateField = driver.findElement(By.id("register-birthdate"));
        birthdateField.clear();
        birthdateField.sendKeys(birthdate);
    }

    @When("I submit the registration form")
    public void i_submit_the_registration_form() {
        WebElement submitButton = driver.findElement(By.id("register-submit-btn"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(d -> {
            String currentUrl = d.getCurrentUrl();
            boolean isRedirected = currentUrl.contains("/explore");
            boolean hasError = !d.findElements(By.id("register-error")).isEmpty();
            return isRedirected || hasError;
        });
    }

    @Then("my account is created in the system")
    public void my_account_is_created_in_the_system() {
        assertTrue(userRepository.findByAccountEmail(testEmail).isPresent(),
                "User with email " + testEmail + " should be created in the database");
    }

    @And("I should be redirected to the dashboard page")
    public void i_should_be_redirected() {
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/explore"),
                "Should be redirected to login page, but current URL is: " + currentUrl);
    }
}
