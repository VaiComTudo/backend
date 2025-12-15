Feature: Test Renter Views Detailed Item Information

	#Test Renter Views Detailed Item Information
	@TEST_VCT-311
	Scenario: Test Renter Views Detailed Item Information
		Given I am viewing the list of filtered bicycles
		When I select a specific bicycle to view details
		Then the system displays comprehensive information including photos, description, and pickup location
		And I can see the item's availability calendar
		And I have the option to proceed with booking
		
