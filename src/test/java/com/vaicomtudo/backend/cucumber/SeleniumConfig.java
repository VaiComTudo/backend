package com.vaicomtudo.backend.cucumber;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;

@Configuration
public class SeleniumConfig {

    private static WebDriver driver;

    @Bean
    @Scope("cucumber-glue")
    public WebDriver webDriver() {
        if (driver == null || isDriverClosed()) {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run in headless mode (no GUI)
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-blink-features=AutomationControlled");
            
            // Set capability to handle unhandled alerts
            options.setCapability("unexpectedAlertBehaviour", "accept");

            driver = new ChromeDriver(options);
        }
        return driver;
    }

    private boolean isDriverClosed() {
        if (driver == null) {
            return true;
        }
        try {
            driver.getTitle();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @PreDestroy
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
            driver = null;
        }
    }
}
