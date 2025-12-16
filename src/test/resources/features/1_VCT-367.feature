Feature: Test Renter Complete Payment Transaction

	@TEST_VCT-367
	Scenario: Test Renter Complete Payment Transaction
		Given my booking has been approved by the owner
		When I proceed to checkout
		And complete payment via the payment system
		Then the payment is securely processed including the rental fee and deposit
		And I receive a digital receipt via email
		And my booking status is updated to "confirmed"
		
