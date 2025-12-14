Feature: Test Owner Removes Item Listing

	@TEST_VCT-243
	Scenario: Test Owner Removes Item Listing
		Given I have a bicycle listed that I want to remove from the platform
		When I select the listing and click "Remove"
		Then the system removes the listing from view
		And the item is no longer available for booking
		And any pending booking requests are cancelled
		
