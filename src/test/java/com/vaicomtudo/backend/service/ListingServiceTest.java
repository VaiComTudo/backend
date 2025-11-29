package com.vaicomtudo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
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

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.exception.IDNotFoundException;
import com.vaicomtudo.backend.exception.MismatchIDException;

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
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = new User();
        owner.setId(ownerId);
        owner.setListings(new HashSet<>());

        listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setOwner(owner);
        listing.setTitle("Test Item");
        listing.setDescription("Test Description");
        listing.setPrice(BigDecimal.valueOf(50.00));
        listing.setState(ListingState.AVAILABLE);
    }

    @Test
    @DisplayName("saveListing should save listing when owner ID matches")
    void whenSaveListing_withMatchingOwnerId_thenListingSaved() {
        // Arrange
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // Act
        Listing savedListing = listingService.saveListing(listing, ownerId);

        // Assert
        assertThat(savedListing).isNotNull();
        assertThat(savedListing.getOwner().getId()).isEqualTo(ownerId);
        verify(userRepository, times(1)).findById(ownerId);
        verify(listingRepository, times(1)).save(listing);
    }

    @Test
    @DisplayName("saveListing should throw MismatchIDException when owner ID does not match")
    void whenSaveListing_withMismatchedOwnerId_thenThrowException() {
        // Arrange
        UUID differentId = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> listingService.saveListing(listing, differentId))
            .isInstanceOf(MismatchIDException.class);

        verify(userRepository, never()).findById(any());
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveListing should throw IDNotFoundException when user not found")
    void whenSaveListing_withNonExistentUser_thenThrowException() {
        // Arrange
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingService.saveListing(listing, ownerId))
            .isInstanceOf(IDNotFoundException.class);

        verify(userRepository, times(1)).findById(ownerId);
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("getListings should return user listings when user exists")
    void whenGetListings_withExistingUser_thenReturnListings() {
        // Arrange
        Set<Listing> listings = new HashSet<>();
        listings.add(listing);
        owner.setListings(listings);
        
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        // Act
        Set<Listing> result = listingService.getListings(ownerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(listing);
    }

    @Test
    @DisplayName("getListings should return empty set when user has no listings")
    void whenGetListings_withNoListings_thenReturnEmptySet() {
        // Arrange
        owner.setListings(new HashSet<>());
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        // Act
        Set<Listing> result = listingService.getListings(ownerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getListings should throw IDNotFoundException when user not found")
    void whenGetListings_withNonExistentUser_thenThrowException() {
        // Arrange
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingService.getListings(ownerId))
            .isInstanceOf(IDNotFoundException.class);

        verify(userRepository, times(1)).findById(ownerId);
    }
}
