Feature: Test Owner Edits Item Listing

	#Test Owner Edits Item Listing
	@TEST_VCT-368
	Scenario: Test Owner Edits Item Listing
		Given I have a bicycle listed and want to update its price
		When I select the bicycle listing and click Edit
		Then I can modify the price, description or title
		And the changes are saved and reflected in the listing visible to renters
		
