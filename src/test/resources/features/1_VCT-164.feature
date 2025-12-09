@REQ_VCT-164
Feature: Item Discovery and Catalog

	@TEST_VCT-164
	Scenario: Test Renter Filters Items by Location
		Given I am browsing available bicycles 
		When I apply the location filter and select "Aveiro"  
		Then the system displays only bicycles available in "Aveiro"  
		And items from other locations are excluded from the result
		
