Feature: Test Renter Books Item for Specific Period

	@TEST_VCT-391
	Scenario: Test Renter Books Item for Specific Period
		Given I have selected a "scooter" that fits my needs  
		When I choose the rental dates "Saturday" and "Sunday" on the calendar  
		Then I submit the booking request to the owner for approval  
		And I wait for the owner's approval
		
