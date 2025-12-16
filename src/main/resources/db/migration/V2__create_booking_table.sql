-- Create Booking table for rental bookings
CREATE TABLE booking (
    id UUID PRIMARY KEY,
    renter_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    pickup_date_time TIMESTAMP NOT NULL,
    dropoff_date_time TIMESTAMP NOT NULL,
    state VARCHAR(50) NOT NULL,
    CONSTRAINT fk_booking_renter FOREIGN KEY (renter_id) REFERENCES users(id),
    CONSTRAINT fk_booking_listing FOREIGN KEY (listing_id) REFERENCES listing(id)
);

-- Create indexes for better query performance
CREATE INDEX idx_booking_renter_id ON booking(renter_id);
CREATE INDEX idx_booking_listing_id ON booking(listing_id);
CREATE INDEX idx_booking_state ON booking(state);
CREATE INDEX idx_booking_dates ON booking(pickup_date_time, dropoff_date_time);
