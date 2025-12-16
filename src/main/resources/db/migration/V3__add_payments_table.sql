-- Add payments table for payment system
CREATE TABLE payment (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    renter_id UUID,
    renter_email VARCHAR(255) NOT NULL,
    rental_fee DECIMAL(19, 2) NOT NULL,
    deposit DECIMAL(19, 2) NOT NULL,
    total DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    receipt_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payment_renter FOREIGN KEY (renter_id) REFERENCES users(id)
);

-- Create indexes for better query performance
CREATE INDEX idx_payment_booking_id ON payment(booking_id);
CREATE INDEX idx_payment_renter_id ON payment(renter_id);
CREATE INDEX idx_payment_status ON payment(status);
