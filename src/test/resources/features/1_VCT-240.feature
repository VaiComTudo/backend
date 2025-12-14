Feature: Test Renter Browses Available Items By Category

	#Test Renter Browses Available Items By Category
	@TEST_VCT-240
	Scenario: Test Renter Browses Available Items By Category
		Given I want to find a bike for a weekend leisure ride
		When I select the category filter and choose "bike"
		Then the system displays all available bicycles with specifications
		And items from other categories are hidden from the results
		
