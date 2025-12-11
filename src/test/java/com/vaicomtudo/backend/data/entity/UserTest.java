package com.vaicomtudo.backend.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class UserTest {

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setName("Test User");
        account.setEmail("test@example.com");
        account.setPasswordHash("hashedpassword");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setAccount(account);
    }

    @Test
    @DisplayName("Should create user with default empty listings set")
    @Requirement("VCT-48")
    void whenCreateUser_thenListingsSetIsEmpty() {
        // Assert
        assertThat(user.getListings()).isNotNull();
        assertThat(user.getListings()).isEmpty();
    }

    @Test
    @DisplayName("Should add listing to user using helper method")
    @Requirement("VCT-48")
    void whenAddListing_thenListingIsAddedAndOwnerIsSet() {
        // Arrange
        Listing listing = new Listing();
        listing.setTitle("Test Item");
        listing.setDescription("Test Description");
        listing.setPrice(BigDecimal.valueOf(50.00));
        listing.setState(ListingState.AVAILABLE);

        // Act
        user.addListing(listing);

        // Assert
        assertThat(user.getListings()).hasSize(1);
        assertThat(user.getListings()).contains(listing);
        assertThat(listing.getOwner()).isSameAs(user);
    }

    @Test
    @DisplayName("Should remove listing from user using helper method")
    @Requirement("VCT-48")
    void whenRemoveListing_thenListingIsRemovedAndOwnerIsNull() {
        // Arrange
        Listing listing = new Listing();
        listing.setTitle("Test Item");
        listing.setDescription("Test Description");
        listing.setPrice(BigDecimal.valueOf(50.00));
        listing.setState(ListingState.AVAILABLE);
        
        user.addListing(listing);

        // Act
        user.removeListing(listing);

        // Assert
        assertThat(user.getListings()).isEmpty();
        assertThat(listing.getOwner()).isNull();
    }

    @Test
    @DisplayName("Should handle multiple listings")
    @Requirement("VCT-48")
    void whenAddMultipleListings_thenAllAreStored() {
        // Arrange
        Listing listing1 = new Listing();
        listing1.setTitle("Item 1");
        listing1.setDescription("Description 1");
        listing1.setPrice(BigDecimal.valueOf(50.00));
        listing1.setState(ListingState.AVAILABLE);

        Listing listing2 = new Listing();
        listing2.setTitle("Item 2");
        listing2.setDescription("Description 2");
        listing2.setPrice(BigDecimal.valueOf(75.00));
        listing2.setState(ListingState.UNAVAILABLE);

        // Act
        user.addListing(listing1);
        user.addListing(listing2);

        // Assert
        assertThat(user.getListings()).hasSize(2);
        assertThat(user.getListings()).contains(listing1, listing2);
        assertThat(listing1.getOwner()).isSameAs(user);
        assertThat(listing2.getOwner()).isSameAs(user);
    }
}
