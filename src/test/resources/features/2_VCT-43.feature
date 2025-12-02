@REQ_VCT-43
Feature: User Management and Authentication
	#* User registration and Login 
	#* Role-based access (renter, owner, etc.) 
	#* Basic profile management 
	#* Rental/Ownership history

	#Tests *As a* registered user *I want to* log in to my account *so that* I can access the platform features.
	@TEST_VCT-107 @REQ_VCT-81
	Scenario: Test Owner/Renter Logs In
		Given I have a registered account
		When I navigate to the login page
		And I enter the email "email@email.com" and password "pass123"
		And I submit the login form
		Then the system authenticates my credentials
		And I am redirected to the homepage
		
