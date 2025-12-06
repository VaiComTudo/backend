@REQ_VCT-60
Feature: Item Management

	@TEST_VCT-128 @REQ_VCT-48
	Scenario: Test Owner Creates New Item Listing
		Given I am logged in
		
		When  I click the "Add New Listing" button

		And Fill in the form with the title "Old child bicycle", the description "An old bicycle from when my daughter was a child.", a daily price of "15" euros, a "GOOD" "bike" vehicle to be picked up and dropped off at "the coffee shop near my house".
		
		Then The system creates the listing
		
