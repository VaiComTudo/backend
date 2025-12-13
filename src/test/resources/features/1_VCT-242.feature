Feature: Tests Renter Filters Items by Price Range

	#Tests Renter Filters Items by Price Range
	@TEST_VCT-242
	Scenario: Tests Renter Filters Items by Price Range
		Given I am browsing available bicycles in "Aveiro"
		When I set the price range filter to up to "20" euros/day
		Then the system displays only bicycles priced at "20" euros/day or less
		And items exceeding my price range are excluded from the results
		And the daily price is clearly displayed for each item
		
