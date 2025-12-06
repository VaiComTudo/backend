@REQ_VCT-41
Feature: Item Discovery and Catalog
	#* Browse and search available transportation items (bikes, scooters, skates, etc.) 
	#* Filter by category, location, price range, and availability 
	#* View detailed item specifications with photos

	#Test Owner Views Listed Items
	@TEST_VCT-146 @REQ_VCT-47
	Scenario: Test Owner Views Listed Items
		Given I am logged into my account
		When I navigate to my listings dashboard
		Then the system displays all my listed items
		And I can see the title, description, price, and condition
		And I can see the item state
		
