package com.vaicomtudo.backend.cucumber.hooks;

import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {

    @Autowired
    private WebDriver driver;

    @Before
    public void beforeScenario(Scenario scenario) {
        // Ensure WebDriver is available before each scenario
        if (driver != null) {
            driver.manage().deleteAllCookies();
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        // Clean up after each scenario but don't quit the driver
        // The driver will be closed by Spring's @PreDestroy
        if (driver != null) {
            try {
                driver.manage().deleteAllCookies();
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }
}
