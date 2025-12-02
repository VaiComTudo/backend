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

    private WebDriver driver;

    @Bean
    @Scope("cucumber-glue")
    public WebDriver webDriver() {
        if (driver == null) {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run kin headless mode (no GUI)
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");

            driver = new ChromeDriver(options);
        }
        return driver;
    }

    @PreDestroy
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
