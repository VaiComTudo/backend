Feature: Test Renter Filters Items By Location

	#Test Renter Filters Items By Location
	@TEST_VCT-241
	Scenario: Test Renter Filters Items By Location
		Given I am browsing available "bikes"
		When I apply the location filter and select "Aveiro"
		Then the system displays only bicycles available in "Aveiro"
		And items from other locations are excluded from the results
		
