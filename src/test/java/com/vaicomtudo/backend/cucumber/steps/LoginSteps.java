package com.vaicomtudo.backend.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.vaicomtudo.backend.data.repository.UserRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.LocalDate;

public class LoginSteps {

    @Autowired
    private WebDriver driver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String testEmail;
    private String testPassword;

    @Given("I have a registered account")
    public void i_have_a_registered_account() {
        userRepository.deleteAll();

        testEmail = "email@email.com";
        testPassword = "pass123";

        Account account = Account.builder()
            .email(testEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Test User")
            .build();

        User user = User.builder()
            .account(account)
            .birthdate(LocalDate.of(2000, 1, 1))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();

        userRepository.save(user);
    }

    @When("I navigate to the login page")
    public void i_navigate_to_the_login_page() {
        driver.get(frontendUrl + "/login");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-form")));
    }

    @And("I enter the email {string} and password {string}")
    public void i_enter_credentials(String email, String password) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-email")));
        emailField.clear();
        emailField.sendKeys(email);

        WebElement passwordField = driver.findElement(By.id("login-password"));
        passwordField.clear();
        passwordField.sendKeys(password);
    }

    @And("I submit the login form")
    public void i_submit_the_login_form() {
        WebElement submitButton = driver.findElement(By.id("login-submit-btn"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(d -> {
            String currentUrl = d.getCurrentUrl();
            boolean isRedirected = currentUrl.contains("/explore");
            boolean hasError = !d.findElements(By.id("login-error")).isEmpty();
            return isRedirected || hasError;
        });
    }

    @Then("the system authenticates my credentials")
    public void the_system_authenticates_my_credentials() {
        boolean hasError = !driver.findElements(By.id("login-error")).isEmpty();
        assertTrue(!hasError, "Authentication should succeed without errors");
    }

    @And("I am redirected to the homepage")
    public void i_am_redirected_to_the_homepage() {
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/explore"),
                "Should be redirected to homepage (/explore), but current URL is: " + currentUrl);
    }
}
