-- Baseline schema for VaiComTudo application
-- Generated from JPA entities

-- Create Account table
CREATE TABLE account (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    photo OID
);

-- Create Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    role VARCHAR(50),
    birthdate DATE NOT NULL,
    account_id UUID,
    rating DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES account(id)
);

-- Create User Availability table (ElementCollection)
CREATE TABLE user_availability (
    user_id UUID NOT NULL,
    start_day VARCHAR(20),
    end_day VARCHAR(20),
    start_time TIME,
    end_time TIME,
    CONSTRAINT fk_user_availability_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create Listing table
CREATE TABLE listing (
    id UUID PRIMARY KEY,
    owner_id UUID,
    description VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    state SMALLINT NOT NULL,
    condition SMALLINT,
    type VARCHAR(255),
    pick_up_location VARCHAR(255) NOT NULL,
    drop_off_location VARCHAR(255) NOT NULL,
    CONSTRAINT fk_listing_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Create Listing Availability table (ElementCollection)
CREATE TABLE listing_availability (
    listing_id UUID NOT NULL,
    start_day VARCHAR(20),
    end_day VARCHAR(20),
    start_time TIME,
    end_time TIME,
    CONSTRAINT fk_listing_availability_listing FOREIGN KEY (listing_id) REFERENCES listing(id)
);

-- Create Listing Photo table
CREATE TABLE listing_photo (
    id BIGSERIAL PRIMARY KEY,
    data OID,
    listing_id UUID,
    CONSTRAINT fk_listing_photo_listing FOREIGN KEY (listing_id) REFERENCES listing(id)
);

-- Create indexes for better query performance
CREATE INDEX idx_users_account_id ON users(account_id);
CREATE INDEX idx_listing_owner_id ON listing(owner_id);
CREATE INDEX idx_listing_state ON listing(state);
CREATE INDEX idx_user_availability_user_id ON user_availability(user_id);
CREATE INDEX idx_listing_availability_listing_id ON listing_availability(listing_id);
CREATE INDEX idx_listing_photo_listing_id ON listing_photo(listing_id);
