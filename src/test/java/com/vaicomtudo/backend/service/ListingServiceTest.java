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

    @Test
    @DisplayName("getAvailableListingsByLocation should return listings matching pickup location")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByLocation_withMatchingPickupLocation_thenReturnListings() {
        // Arrange
        String location = "Lisboa";
        Listing lisboaListing = new Listing();
        lisboaListing.setId(UUID.randomUUID());
        lisboaListing.setOwner(owner);
        lisboaListing.setTitle("Bike in Lisboa");
        lisboaListing.setDescription("Bike rental");
        lisboaListing.setPrice(BigDecimal.valueOf(25.00));
        lisboaListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        lisboaListing.setVehicle(vehicle);
        lisboaListing.setPickUpLocation("Lisboa");
        lisboaListing.setDropOffLocation("Porto");

        List<Listing> pickupResults = new ArrayList<>();
        pickupResults.add(lisboaListing);
        List<Listing> dropoffResults = new ArrayList<>();

        when(listingRepository.findByStateAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByLocation(location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(lisboaListing);
        assertThat(result.get(0).getPickUpLocation()).containsIgnoringCase(location);
        verify(listingRepository, times(1))
                .findByStateAndPickUpLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
        verify(listingRepository, times(1))
                .findByStateAndDropOffLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
    }

    @Test
    @DisplayName("getAvailableListingsByLocation should return listings matching dropoff location")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByLocation_withMatchingDropoffLocation_thenReturnListings() {
        // Arrange
        String location = "Porto";
        Listing portoListing = new Listing();
        portoListing.setId(UUID.randomUUID());
        portoListing.setOwner(owner);
        portoListing.setTitle("Bike to Porto");
        portoListing.setDescription("Bike rental");
        portoListing.setPrice(BigDecimal.valueOf(30.00));
        portoListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        portoListing.setVehicle(vehicle);
        portoListing.setPickUpLocation("Lisboa");
        portoListing.setDropOffLocation("Porto");

        List<Listing> pickupResults = new ArrayList<>();
        List<Listing> dropoffResults = new ArrayList<>();
        dropoffResults.add(portoListing);

        when(listingRepository.findByStateAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByLocation(location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(portoListing);
        assertThat(result.get(0).getDropOffLocation()).containsIgnoringCase(location);
        verify(listingRepository, times(1))
                .findByStateAndPickUpLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
        verify(listingRepository, times(1))
                .findByStateAndDropOffLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
    }

    @Test
    @DisplayName("getAvailableListingsByLocation should combine pickup and dropoff results and remove duplicates")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByLocation_withMatchingBothLocations_thenReturnCombinedWithoutDuplicates() {
        // Arrange
        String location = "Lisboa";
        Listing lisboaListing = new Listing();
        lisboaListing.setId(UUID.randomUUID());
        lisboaListing.setOwner(owner);
        lisboaListing.setTitle("Bike in Lisboa");
        lisboaListing.setDescription("Bike rental");
        lisboaListing.setPrice(BigDecimal.valueOf(25.00));
        lisboaListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        lisboaListing.setVehicle(vehicle);
        lisboaListing.setPickUpLocation("Lisboa");
        lisboaListing.setDropOffLocation("Lisboa");

        // Same listing appears in both results (pickup and dropoff)
        List<Listing> pickupResults = new ArrayList<>();
        pickupResults.add(lisboaListing);
        List<Listing> dropoffResults = new ArrayList<>();
        dropoffResults.add(lisboaListing);

        when(listingRepository.findByStateAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByLocation(location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1); // Should be deduplicated
        assertThat(result).contains(lisboaListing);
        verify(listingRepository, times(1))
                .findByStateAndPickUpLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
        verify(listingRepository, times(1))
                .findByStateAndDropOffLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
    }

    @Test
    @DisplayName("getAvailableListingsByLocation should return empty list when no listings match")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByLocation_withNoMatchingListings_thenReturnEmptyList() {
        // Arrange
        String location = "Faro";
        when(listingRepository.findByStateAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(new ArrayList<>());
        when(listingRepository.findByStateAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(new ArrayList<>());

        // Act
        List<Listing> result = listingService.getAvailableListingsByLocation(location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(listingRepository, times(1))
                .findByStateAndPickUpLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
        verify(listingRepository, times(1))
                .findByStateAndDropOffLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
    }

    @Test
    @DisplayName("getAvailableListingsByLocation should be case-insensitive")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByLocation_withCaseInsensitiveSearch_thenReturnListings() {
        // Arrange
        String location = "lisboa"; // lowercase
        Listing lisboaListing = new Listing();
        lisboaListing.setId(UUID.randomUUID());
        lisboaListing.setOwner(owner);
        lisboaListing.setTitle("Bike in Lisboa");
        lisboaListing.setDescription("Bike rental");
        lisboaListing.setPrice(BigDecimal.valueOf(25.00));
        lisboaListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        lisboaListing.setVehicle(vehicle);
        lisboaListing.setPickUpLocation("Lisboa"); // uppercase in database
        lisboaListing.setDropOffLocation("Porto");

        List<Listing> pickupResults = new ArrayList<>();
        pickupResults.add(lisboaListing);
        List<Listing> dropoffResults = new ArrayList<>();

        when(listingRepository.findByStateAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByLocation(location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(lisboaListing);
        verify(listingRepository, times(1))
                .findByStateAndPickUpLocationContainingIgnoreCase(ListingState.AVAILABLE, location);
    }

    @Test
    @DisplayName("getAvailableListingsByCategoryAndLocation should return listings matching category and pickup location")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByCategoryAndLocation_withMatchingCategoryAndPickup_thenReturnListings() {
        // Arrange
        String category = "bicycle";
        String location = "Lisboa";
        Listing lisboaBikeListing = new Listing();
        lisboaBikeListing.setId(UUID.randomUUID());
        lisboaBikeListing.setOwner(owner);
        lisboaBikeListing.setTitle("Bike in Lisboa");
        lisboaBikeListing.setDescription("Bike rental");
        lisboaBikeListing.setPrice(BigDecimal.valueOf(25.00));
        lisboaBikeListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        lisboaBikeListing.setVehicle(vehicle);
        lisboaBikeListing.setPickUpLocation("Lisboa");
        lisboaBikeListing.setDropOffLocation("Porto");

        List<Listing> pickupResults = new ArrayList<>();
        pickupResults.add(lisboaBikeListing);
        List<Listing> dropoffResults = new ArrayList<>();

        when(listingRepository.findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByCategoryAndLocation(category, location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(lisboaBikeListing);
        assertThat(result.get(0).getVehicle().getType()).isEqualTo(category);
        assertThat(result.get(0).getPickUpLocation()).containsIgnoringCase(location);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
    }

    @Test
    @DisplayName("getAvailableListingsByCategoryAndLocation should return listings matching category and dropoff location")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByCategoryAndLocation_withMatchingCategoryAndDropoff_thenReturnListings() {
        // Arrange
        String category = "scooter";
        String location = "Porto";
        Listing portoScooterListing = new Listing();
        portoScooterListing.setId(UUID.randomUUID());
        portoScooterListing.setOwner(owner);
        portoScooterListing.setTitle("Scooter to Porto");
        portoScooterListing.setDescription("Scooter rental");
        portoScooterListing.setPrice(BigDecimal.valueOf(30.00));
        portoScooterListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("scooter");
        vehicle.setCondition(VehicleCondition.EXCELLENT);
        portoScooterListing.setVehicle(vehicle);
        portoScooterListing.setPickUpLocation("Lisboa");
        portoScooterListing.setDropOffLocation("Porto");

        List<Listing> pickupResults = new ArrayList<>();
        List<Listing> dropoffResults = new ArrayList<>();
        dropoffResults.add(portoScooterListing);

        when(listingRepository.findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByCategoryAndLocation(category, location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(portoScooterListing);
        assertThat(result.get(0).getVehicle().getType()).isEqualTo(category);
        assertThat(result.get(0).getDropOffLocation()).containsIgnoringCase(location);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
    }

    @Test
    @DisplayName("getAvailableListingsByCategoryAndLocation should combine results and remove duplicates")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByCategoryAndLocation_withMatchingBoth_thenReturnCombinedWithoutDuplicates() {
        // Arrange
        String category = "bicycle";
        String location = "Lisboa";
        Listing lisboaBikeListing = new Listing();
        lisboaBikeListing.setId(UUID.randomUUID());
        lisboaBikeListing.setOwner(owner);
        lisboaBikeListing.setTitle("Bike in Lisboa");
        lisboaBikeListing.setDescription("Bike rental");
        lisboaBikeListing.setPrice(BigDecimal.valueOf(25.00));
        lisboaBikeListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        lisboaBikeListing.setVehicle(vehicle);
        lisboaBikeListing.setPickUpLocation("Lisboa");
        lisboaBikeListing.setDropOffLocation("Lisboa");

        // Same listing appears in both results
        List<Listing> pickupResults = new ArrayList<>();
        pickupResults.add(lisboaBikeListing);
        List<Listing> dropoffResults = new ArrayList<>();
        dropoffResults.add(lisboaBikeListing);

        when(listingRepository.findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByCategoryAndLocation(category, location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1); // Should be deduplicated
        assertThat(result).contains(lisboaBikeListing);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
    }

    @Test
    @DisplayName("getAvailableListingsByCategoryAndLocation should return empty list when no listings match")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByCategoryAndLocation_withNoMatchingListings_thenReturnEmptyList() {
        // Arrange
        String category = "skate";
        String location = "Faro";
        when(listingRepository.findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(new ArrayList<>());
        when(listingRepository.findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(new ArrayList<>());

        // Act
        List<Listing> result = listingService.getAvailableListingsByCategoryAndLocation(category, location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
    }

    @Test
    @DisplayName("getAvailableListingsByCategoryAndLocation should be case-insensitive for location")
    @Requirement("VCT-33")
    void whenGetAvailableListingsByCategoryAndLocation_withCaseInsensitiveLocation_thenReturnListings() {
        // Arrange
        String category = "bicycle";
        String location = "lisboa"; // lowercase
        Listing lisboaBikeListing = new Listing();
        lisboaBikeListing.setId(UUID.randomUUID());
        lisboaBikeListing.setOwner(owner);
        lisboaBikeListing.setTitle("Bike in Lisboa");
        lisboaBikeListing.setDescription("Bike rental");
        lisboaBikeListing.setPrice(BigDecimal.valueOf(25.00));
        lisboaBikeListing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        lisboaBikeListing.setVehicle(vehicle);
        lisboaBikeListing.setPickUpLocation("Lisboa"); // uppercase in database
        lisboaBikeListing.setDropOffLocation("Porto");

        List<Listing> pickupResults = new ArrayList<>();
        pickupResults.add(lisboaBikeListing);
        List<Listing> dropoffResults = new ArrayList<>();

        when(listingRepository.findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(pickupResults);
        when(listingRepository.findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location))
                .thenReturn(dropoffResults);

        // Act
        List<Listing> result = listingService.getAvailableListingsByCategoryAndLocation(category, location);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(lisboaBikeListing);
        verify(listingRepository, times(1))
                .findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);
    }
}
