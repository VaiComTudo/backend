@VCT-80
Feature: User Registration
  As a new user visiting the platform
  I want to register an account
  So that I can access the platform services

  Scenario: Successful user registration
    Given I am a new user visiting the platform
    When I navigate to the registration page
    And I provide my email "newuser@example.com", password "SecurePass123", name "John Doe", and birthdate "2000-01-01"
    And I submit the registration form
    Then my account is created in the system
    And I should be redirected to the login page or dashboard
