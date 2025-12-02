@REQ_VCT-43
Feature: User Management and Authentication
	#* User registration and Login 
	#* Role-based access (renter, owner, etc.) 
	#* Basic profile management 
	#* Rental/Ownership history

	#Tests *As a* new user *I want to* register an account on the platform *so that* I can access the rental services.
	@TEST_VCT-102 @REQ_VCT-80
	Scenario: Test Owner/Renter Registers a New Account
		Given I am a new user visiting the platform
		When I navigate to the registration page
		And I provide my email "newuser@example.com", password "SecurePass123", name "John Doe", and birthdate "2000-01-01"
		And I submit the registration form
		Then my account is created in the system
		And I should be redirected to the login page
		
