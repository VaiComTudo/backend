package com.vaicomtudo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.exception.EmailNotFoundException;
import com.vaicomtudo.backend.exception.MismatchEmailException;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingService listingService;

    private User owner;
    private Listing listing;
    private String ownerEmail;

    @BeforeEach
    void setUp() {
        ownerEmail = "owner@email.com";

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setEmail(ownerEmail);

        owner = new User();
        owner.setAccount(account);
        owner.setId(account.getId());
        owner.setListings(new HashSet<>());

        listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setOwner(owner);
        listing.setTitle("Test Item");
        listing.setDescription("Test Description");
        listing.setPrice(BigDecimal.valueOf(50.00));
        listing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("Car");
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Test Pickup");
        listing.setDropOffLocation("Test Dropoff");
    }

    @Test
    @DisplayName("saveListing should save listing when owner email matches")
    @Requirement("VCT-48")
    void whenSaveListing_withMatchingOwnerEmail_thenListingSaved() {
        // Arrange
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // Act
        Listing savedListing = listingService.saveListing(listing, ownerEmail);

        // Assert
        assertThat(savedListing).isNotNull();
        assertThat(savedListing.getOwner().getAccount().getEmail()).isEqualTo(ownerEmail);
        verify(userRepository, times(1)).findByAccountEmail(ownerEmail);
        verify(listingRepository, times(1)).save(listing);
    }

    @Test
    @DisplayName("saveListing should throw MismatchEmailException when owner email does not match")
    @Requirement("VCT-48")
    void whenSaveListing_withMismatchedOwnerEmail_thenThrowException() {
        // Arrange
        String differentEmail = "email2@testing.com";

        // Act & Assert
        assertThatThrownBy(() -> listingService.saveListing(listing, differentEmail))
                .isInstanceOf(MismatchEmailException.class);

        verify(userRepository, never()).findByAccountEmail(any());
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveListing should throw EmailNotFoundException when user not found")
    @Requirement("VCT-48")
    void whenSaveListing_withNonExistentUser_thenThrowException() {
        // Arrange
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingService.saveListing(listing, ownerEmail))
                .isInstanceOf(EmailNotFoundException.class);

        verify(userRepository, times(1)).findByAccountEmail(ownerEmail);
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("getListings should return user listings when user exists")
    @Requirement("VCT-48")
    void whenGetListings_withExistingUser_thenReturnListings() {
        // Arrange
        Set<Listing> listings = new HashSet<>();
        listings.add(listing);
        owner.setListings(listings);

        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));

        // Act
        Set<Listing> result = listingService.getListings(ownerEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(listing);
    }

    @Test
    @DisplayName("getListings should return empty set when user has no listings")
    @Requirement("VCT-48")
    void whenGetListings_withNoListings_thenReturnEmptySet() {
        // Arrange
        owner.setListings(new HashSet<>());
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));

        // Act
        Set<Listing> result = listingService.getListings(ownerEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getListings should throw EmailNotFoundException when user not found")
    @Requirement("VCT-48")
    void whenGetListings_withNonExistentUser_thenThrowException() {
        // Arrange
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingService.getListings(ownerEmail))
                .isInstanceOf(EmailNotFoundException.class);

        verify(userRepository, times(1)).findByAccountEmail(ownerEmail);
    }

    @Test
    @DisplayName("getAvailableListingsByCategory should return available listings for given category")
    @Requirement("VCT-32")
    void whenGetAvailableListingsByCategory_withValidCategory_thenReturnListings() {
        // Arrange
        String category = "bicycle";
        Listing bicycleListing = new Listing();
        bicycleListing.setId(UUID.randomUUID());
        bicycleListing.setOwner(owner);
        bicycleListing.setTitle("Bicycle Rental");
        bicycleListing.setDescription("Mountain bike");
        bicycleListing.setPrice(BigDecimal.valueOf(25.00));
        bicycleListing.setState(ListingState.AVAILABLE);

        Vehicle bicycleVehicle = new Vehicle();
        bicycleVehicle.setType("bicycle");
        bicycleVehicle.setCondition(VehicleCondition.GOOD);
        bicycleListing.setVehicle(bicycleVehicle);
        bicycleListing.setPickUpLocation("Location A");
        bicycleListing.setDropOffLocation("Location B");

        List<Listing> availableListings = new ArrayList<>();
        availableListings.add(bicycleListing);

        when(listingRepository.findByStateAndVehicleType(ListingState.AVAILABLE, category))
                .thenReturn(availableListings);

        // Act
        List<Listing> result = listingService.getAvailableListingsByCategory(category);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(bicycleListing);
        assertThat(result.get(0).getState()).isEqualTo(ListingState.AVAILABLE);
        assertThat(result.get(0).getVehicle().getType()).isEqualTo("bicycle");
        verify(listingRepository, times(1)).findByStateAndVehicleType(ListingState.AVAILABLE, category);
    }

    @Test
    @DisplayName("getAvailableListingsByCategory should return empty list when no listings found")
    @Requirement("VCT-32")
    void whenGetAvailableListingsByCategory_withNoMatchingListings_thenReturnEmptyList() {
        // Arrange
        String category = "scooter";
        when(listingRepository.findByStateAndVehicleType(ListingState.AVAILABLE, category))
                .thenReturn(new ArrayList<>());

        // Act
        List<Listing> result = listingService.getAvailableListingsByCategory(category);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(listingRepository, times(1)).findByStateAndVehicleType(ListingState.AVAILABLE, category);
    }
}
