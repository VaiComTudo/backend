package com.vaicomtudo.backend.boundary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class RenterControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create account
        Account account = new Account();
        account.setName("Integration Test Owner");
        account.setEmail("renter@test.com");
        account.setPasswordHash("hashedpassword");

        // Save account first (no longer cascades from user)
        account = accountRepository.save(account);

        // Create and save user
        owner = new User();
        owner.setAccount(account);
        owner.setBirthdate(LocalDate.of(1990, 1, 1));
        owner = userRepository.save(owner);
    }

    @Test
    @DisplayName("Integration test: Filter listings by bicycle category")
    @Requirement("VCT-32")
    void whenFilterByBicycleCategory_thenReturnsOnlyBicycles() throws Exception {
        // Create bicycle listing
        Listing bicycleListing = new Listing();
        bicycleListing.setOwner(owner);
        bicycleListing.setTitle("Mountain Bike");
        bicycleListing.setDescription("Great mountain bike");
        bicycleListing.setPrice(BigDecimal.valueOf(25.00));
        bicycleListing.setState(ListingState.AVAILABLE);

        Vehicle bicycleVehicle = new Vehicle();
        bicycleVehicle.setType("bicycle");
        bicycleVehicle.setCondition(VehicleCondition.GOOD);
        bicycleListing.setVehicle(bicycleVehicle);
        bicycleListing.setPickUpLocation("Location A");
        bicycleListing.setDropOffLocation("Location B");
        listingRepository.save(bicycleListing);

        // Create scooter listing
        Listing scooterListing = new Listing();
        scooterListing.setOwner(owner);
        scooterListing.setTitle("Electric Scooter");
        scooterListing.setDescription("Fast electric scooter");
        scooterListing.setPrice(BigDecimal.valueOf(30.00));
        scooterListing.setState(ListingState.AVAILABLE);

        Vehicle scooterVehicle = new Vehicle();
        scooterVehicle.setType("scooter");
        scooterVehicle.setCondition(VehicleCondition.EXCELLENT);
        scooterListing.setVehicle(scooterVehicle);
        scooterListing.setPickUpLocation("Location C");
        scooterListing.setDropOffLocation("Location D");
        listingRepository.save(scooterListing);

        // Filter by bicycle category
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("category", "bicycle")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Mountain Bike"))
            .andExpect(jsonPath("$.content[0].vehicle.type").value("bicycle"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("Integration test: Only available listings are returned")
    @Requirement("VCT-32")
    void whenFilterByCategory_thenReturnsOnlyAvailableListings() throws Exception {
        // Create available bicycle listing
        Listing availableBicycle = new Listing();
        availableBicycle.setOwner(owner);
        availableBicycle.setTitle("Available Bike");
        availableBicycle.setDescription("This bike is available");
        availableBicycle.setPrice(BigDecimal.valueOf(25.00));
        availableBicycle.setState(ListingState.AVAILABLE);

        Vehicle availableVehicle = new Vehicle();
        availableVehicle.setType("bicycle");
        availableVehicle.setCondition(VehicleCondition.GOOD);
        availableBicycle.setVehicle(availableVehicle);
        availableBicycle.setPickUpLocation("Location A");
        availableBicycle.setDropOffLocation("Location B");
        listingRepository.save(availableBicycle);

        // Create unavailable bicycle listing
        Listing unavailableBicycle = new Listing();
        unavailableBicycle.setOwner(owner);
        unavailableBicycle.setTitle("Unavailable Bike");
        unavailableBicycle.setDescription("This bike is not available");
        unavailableBicycle.setPrice(BigDecimal.valueOf(25.00));
        unavailableBicycle.setState(ListingState.UNAVAILABLE);

        Vehicle unavailableVehicle = new Vehicle();
        unavailableVehicle.setType("bicycle");
        unavailableVehicle.setCondition(VehicleCondition.GOOD);
        unavailableBicycle.setVehicle(unavailableVehicle);
        unavailableBicycle.setPickUpLocation("Location C");
        unavailableBicycle.setDropOffLocation("Location D");
        listingRepository.save(unavailableBicycle);

        // Filter by bicycle category
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("category", "bicycle")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Available Bike"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Integration test: Multiple listings of same category are returned")
    @Requirement("VCT-32")
    void whenFilterByCategory_withMultipleListings_thenReturnsAllMatching() throws Exception {
        // Create multiple bicycle listings
        for (int i = 1; i <= 3; i++) {
            Listing listing = new Listing();
            listing.setOwner(owner);
            listing.setTitle("Bike " + i);
            listing.setDescription("Bicycle number " + i);
            listing.setPrice(BigDecimal.valueOf(20.00 + i));
            listing.setState(ListingState.AVAILABLE);

            Vehicle vehicle = new Vehicle();
            vehicle.setType("bicycle");
            vehicle.setCondition(VehicleCondition.GOOD);
            listing.setVehicle(vehicle);
            listing.setPickUpLocation("Pickup " + i);
            listing.setDropOffLocation("Dropoff " + i);
            listingRepository.save(listing);
        }

        // Filter by bicycle category
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("category", "bicycle")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[0].vehicle.type").value("bicycle"))
            .andExpect(jsonPath("$.content[1].vehicle.type").value("bicycle"))
            .andExpect(jsonPath("$.content[2].vehicle.type").value("bicycle"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("Integration test: Filter listings by location")
    @Requirement("VCT-33")
    void whenFilterByLocation_thenReturnsMatchingListings() throws Exception {
        // Create listings in different locations
        Listing aveiroListing = new Listing();
        aveiroListing.setOwner(owner);
        aveiroListing.setTitle("Bike in Aveiro");
        aveiroListing.setDescription("Available in Aveiro");
        aveiroListing.setPrice(BigDecimal.valueOf(20.00));
        aveiroListing.setState(ListingState.AVAILABLE);
        Vehicle aveiroVehicle = new Vehicle();
        aveiroVehicle.setType("bicycle");
        aveiroVehicle.setCondition(VehicleCondition.GOOD);
        aveiroListing.setVehicle(aveiroVehicle);
        aveiroListing.setPickUpLocation("Aveiro Centro");
        aveiroListing.setDropOffLocation("Aveiro Station");
        listingRepository.save(aveiroListing);

        Listing portoListing = new Listing();
        portoListing.setOwner(owner);
        portoListing.setTitle("Bike in Porto");
        portoListing.setDescription("Available in Porto");
        portoListing.setPrice(BigDecimal.valueOf(25.00));
        portoListing.setState(ListingState.AVAILABLE);
        Vehicle portoVehicle = new Vehicle();
        portoVehicle.setType("bicycle");
        portoVehicle.setCondition(VehicleCondition.GOOD);
        portoListing.setVehicle(portoVehicle);
        portoListing.setPickUpLocation("Porto Downtown");
        portoListing.setDropOffLocation("Porto Airport");
        listingRepository.save(portoListing);

        // Filter by Aveiro location
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("location", "Aveiro")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Bike in Aveiro"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Integration test: Filter listings by price range")
    @Requirement("VCT-34")
    void whenFilterByPriceRange_thenReturnsMatchingListings() throws Exception {
        // Create listings with different prices
        Listing cheapListing = new Listing();
        cheapListing.setOwner(owner);
        cheapListing.setTitle("Cheap Bike");
        cheapListing.setDescription("Budget option");
        cheapListing.setPrice(BigDecimal.valueOf(10.00));
        cheapListing.setState(ListingState.AVAILABLE);
        Vehicle cheapVehicle = new Vehicle();
        cheapVehicle.setType("bicycle");
        cheapVehicle.setCondition(VehicleCondition.NEEDS_WORK);
        cheapListing.setVehicle(cheapVehicle);
        cheapListing.setPickUpLocation("Location A");
        cheapListing.setDropOffLocation("Location B");
        listingRepository.save(cheapListing);

        Listing midListing = new Listing();
        midListing.setOwner(owner);
        midListing.setTitle("Mid-range Bike");
        midListing.setDescription("Good quality");
        midListing.setPrice(BigDecimal.valueOf(25.00));
        midListing.setState(ListingState.AVAILABLE);
        Vehicle midVehicle = new Vehicle();
        midVehicle.setType("bicycle");
        midVehicle.setCondition(VehicleCondition.GOOD);
        midListing.setVehicle(midVehicle);
        midListing.setPickUpLocation("Location C");
        midListing.setDropOffLocation("Location D");
        listingRepository.save(midListing);

        Listing expensiveListing = new Listing();
        expensiveListing.setOwner(owner);
        expensiveListing.setTitle("Premium Bike");
        expensiveListing.setDescription("Top quality");
        expensiveListing.setPrice(BigDecimal.valueOf(50.00));
        expensiveListing.setState(ListingState.AVAILABLE);
        Vehicle expensiveVehicle = new Vehicle();
        expensiveVehicle.setType("bicycle");
        expensiveVehicle.setCondition(VehicleCondition.EXCELLENT);
        expensiveListing.setVehicle(expensiveVehicle);
        expensiveListing.setPickUpLocation("Location E");
        expensiveListing.setDropOffLocation("Location F");
        listingRepository.save(expensiveListing);

        // Filter by price range 20-30
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("minPrice", "20")
            .param("maxPrice", "30")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Mid-range Bike"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Integration test: Authenticated user does not see own listings")
    @Requirement("VCT-33")
    @WithMockUser(username = "renter@test.com")
    void whenAuthenticated_thenOwnListingsAreExcluded() throws Exception {
        // Create a listing owned by the authenticated user
        Listing ownListing = new Listing();
        ownListing.setOwner(owner);
        ownListing.setTitle("Own Listing");
        ownListing.setDescription("Should not appear");
        ownListing.setPrice(BigDecimal.valueOf(100.00));
        ownListing.setState(ListingState.AVAILABLE);
        Vehicle v = new Vehicle();
        v.setType("bicycle");
        v.setCondition(VehicleCondition.GOOD);
        ownListing.setVehicle(v);
        ownListing.setPickUpLocation("Somewhere");
        ownListing.setDropOffLocation("Elsewhere");
        listingRepository.save(ownListing);
        // Act & Assert
        mockMvc.perform(get("/api/v1/renters/listings")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(0));
    }
    @Test
    @DisplayName("Integration test: Unauthenticated user sees all listings")
    @Requirement("VCT-33")
    void whenUnauthenticated_thenAllListingsAreReturned() throws Exception {
        // Create a listing owned by a user
        Listing publicListing = new Listing();
        publicListing.setOwner(owner);
        publicListing.setTitle("Public Listing");
        publicListing.setDescription("Should appear");
        publicListing.setPrice(BigDecimal.valueOf(100.00));
        publicListing.setState(ListingState.AVAILABLE);
        Vehicle v = new Vehicle();
        v.setType("bicycle");
        v.setCondition(VehicleCondition.GOOD);
        publicListing.setVehicle(v);
        publicListing.setPickUpLocation("Somewhere");
        publicListing.setDropOffLocation("Elsewhere");
        listingRepository.save(publicListing);
        // Act & Assert
        mockMvc.perform(get("/api/v1/renters/listings")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1));
    }
    @Test
    @DisplayName("Integration test: Filter listings by combined criteria")
    @Requirement("VCT-34")
    void whenFilterByCombinedCriteria_thenReturnsMatchingListings() throws Exception {
        // Create bicycle listing matching all criteria
        Listing matchingListing = new Listing();
        matchingListing.setOwner(owner);
        matchingListing.setTitle("Perfect Bike");
        matchingListing.setDescription("Matches all criteria");
        matchingListing.setPrice(BigDecimal.valueOf(25.00));
        matchingListing.setState(ListingState.AVAILABLE);
        Vehicle matchingVehicle = new Vehicle();
        matchingVehicle.setType("bicycle");
        matchingVehicle.setCondition(VehicleCondition.EXCELLENT);
        matchingListing.setVehicle(matchingVehicle);
        matchingListing.setPickUpLocation("Aveiro Centro");
        matchingListing.setDropOffLocation("Aveiro Station");
        listingRepository.save(matchingListing);

        // Create scooter listing that won't match
        Listing nonMatchingListing = new Listing();
        nonMatchingListing.setOwner(owner);
        nonMatchingListing.setTitle("Aveiro Scooter");
        nonMatchingListing.setDescription("Wrong category");
        nonMatchingListing.setPrice(BigDecimal.valueOf(25.00));
        nonMatchingListing.setState(ListingState.AVAILABLE);
        Vehicle nonMatchingVehicle = new Vehicle();
        nonMatchingVehicle.setType("scooter");
        nonMatchingVehicle.setCondition(VehicleCondition.GOOD);
        nonMatchingListing.setVehicle(nonMatchingVehicle);
        nonMatchingListing.setPickUpLocation("Aveiro Centro");
        nonMatchingListing.setDropOffLocation("Aveiro Station");
        listingRepository.save(nonMatchingListing);

        // Filter by category=bicycle, location=Aveiro, price 20-30
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("category", "bicycle")
            .param("location", "Aveiro")
            .param("minPrice", "20")
            .param("maxPrice", "30")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Perfect Bike"))
            .andExpect(jsonPath("$.content[0].vehicle.type").value("bicycle"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Integration test: Authenticated user sees other users' listings but not own")
    @Requirement("VCT-33")
    @WithMockUser(username = "renter@test.com")
    void whenAuthenticatedWithCategory_thenOwnListingsAreExcludedButOthersShown() throws Exception {
        // Create another user
        Account otherAccount = new Account();
        otherAccount.setName("Other User");
        otherAccount.setEmail("other@test.com");
        otherAccount.setPasswordHash("hashedpassword");
        otherAccount = accountRepository.save(otherAccount);

        User otherUser = new User();
        otherUser.setAccount(otherAccount);
        otherUser.setBirthdate(LocalDate.of(1995, 5, 15));
        otherUser = userRepository.save(otherUser);

        // Create listing by authenticated user (should be excluded)
        Listing ownListing = new Listing();
        ownListing.setOwner(owner);
        ownListing.setTitle("Own Bicycle");
        ownListing.setDescription("Should not appear");
        ownListing.setPrice(BigDecimal.valueOf(20.00));
        ownListing.setState(ListingState.AVAILABLE);
        Vehicle ownVehicle = new Vehicle();
        ownVehicle.setType("bicycle");
        ownVehicle.setCondition(VehicleCondition.GOOD);
        ownListing.setVehicle(ownVehicle);
        ownListing.setPickUpLocation("Location A");
        ownListing.setDropOffLocation("Location B");
        listingRepository.save(ownListing);

        // Create listing by other user (should appear)
        Listing otherListing = new Listing();
        otherListing.setOwner(otherUser);
        otherListing.setTitle("Other's Bicycle");
        otherListing.setDescription("Should appear");
        otherListing.setPrice(BigDecimal.valueOf(25.00));
        otherListing.setState(ListingState.AVAILABLE);
        Vehicle otherVehicle = new Vehicle();
        otherVehicle.setType("bicycle");
        otherVehicle.setCondition(VehicleCondition.GOOD);
        otherListing.setVehicle(otherVehicle);
        otherListing.setPickUpLocation("Location C");
        otherListing.setDropOffLocation("Location D");
        listingRepository.save(otherListing);

        // Filter by bicycle category as authenticated user
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("category", "bicycle")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Other's Bicycle"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Integration test: Authenticated user with combined filters excludes own listings")
    @Requirement("VCT-34")
    @WithMockUser(username = "renter@test.com")
    void whenAuthenticatedWithCombinedFilters_thenOwnListingsAreExcluded() throws Exception {
        // Create another user
        Account otherAccount = new Account();
        otherAccount.setName("Other User");
        otherAccount.setEmail("other@test.com");
        otherAccount.setPasswordHash("hashedpassword");
        otherAccount = accountRepository.save(otherAccount);

        User otherUser = new User();
        otherUser.setAccount(otherAccount);
        otherUser.setBirthdate(LocalDate.of(1995, 5, 15));
        otherUser = userRepository.save(otherUser);

        // Create listing by authenticated user matching criteria (should be excluded)
        Listing ownListing = new Listing();
        ownListing.setOwner(owner);
        ownListing.setTitle("Own Perfect Bike");
        ownListing.setDescription("Matches but should not appear");
        ownListing.setPrice(BigDecimal.valueOf(25.00));
        ownListing.setState(ListingState.AVAILABLE);
        Vehicle ownVehicle = new Vehicle();
        ownVehicle.setType("bicycle");
        ownVehicle.setCondition(VehicleCondition.GOOD);
        ownListing.setVehicle(ownVehicle);
        ownListing.setPickUpLocation("Aveiro Centro");
        ownListing.setDropOffLocation("Aveiro Station");
        listingRepository.save(ownListing);

        // Create listing by other user matching criteria (should appear)
        Listing otherListing = new Listing();
        otherListing.setOwner(otherUser);
        otherListing.setTitle("Other's Perfect Bike");
        otherListing.setDescription("Matches and should appear");
        otherListing.setPrice(BigDecimal.valueOf(25.00));
        otherListing.setState(ListingState.AVAILABLE);
        Vehicle otherVehicle = new Vehicle();
        otherVehicle.setType("bicycle");
        otherVehicle.setCondition(VehicleCondition.EXCELLENT);
        otherListing.setVehicle(otherVehicle);
        otherListing.setPickUpLocation("Aveiro Downtown");
        otherListing.setDropOffLocation("Aveiro Port");
        listingRepository.save(otherListing);

        // Filter with combined criteria as authenticated user
        mockMvc.perform(get("/api/v1/renters/listings")
            .param("category", "bicycle")
            .param("location", "Aveiro")
            .param("minPrice", "20")
            .param("maxPrice", "30")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Other's Perfect Bike"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Integration test: Get listing by ID returns complete listing details")
    @Requirement("VCT-35")
    void whenGetListingById_thenReturnsCompleteListingDetails() throws Exception {
        // Create a listing
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Detailed Mountain Bike");
        listing.setDescription("A bike with complete information");
        listing.setPrice(BigDecimal.valueOf(35.50));
        listing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.EXCELLENT);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Aveiro Centro");
        listing.setDropOffLocation("Aveiro Station");
        
        listing = listingRepository.save(listing);
        String listingId = listing.getId().toString();

        // Retrieve listing by ID
        mockMvc.perform(get("/api/v1/renters/listings/{id}", listingId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(listingId))
            .andExpect(jsonPath("$.title").value("Detailed Mountain Bike"))
            .andExpect(jsonPath("$.description").value("A bike with complete information"))
            .andExpect(jsonPath("$.price").value(35.50))
            .andExpect(jsonPath("$.state").value("AVAILABLE"))
            .andExpect(jsonPath("$.vehicle.type").value("bicycle"))
            .andExpect(jsonPath("$.vehicle.condition").value("EXCELLENT"))
            .andExpect(jsonPath("$.pickUpLocation").value("Aveiro Centro"))
            .andExpect(jsonPath("$.dropOffLocation").value("Aveiro Station"));
    }

    @Test
    @DisplayName("Integration test: Get listing by non-existent ID returns 404")
    @Requirement("VCT-35")
    void whenGetListingById_withNonExistentId_thenReturnsNotFound() throws Exception {
        // Use a random UUID that doesn't exist
        String nonExistentId = java.util.UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/renters/listings/{id}", nonExistentId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration test: Get listing by ID returns listing regardless of state")
    @Requirement("VCT-35")
    void whenGetListingById_withUnavailableListing_thenReturnsListing() throws Exception {
        // Create an unavailable listing
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Unavailable Bike");
        listing.setDescription("This bike is not available");
        listing.setPrice(BigDecimal.valueOf(20.00));
        listing.setState(ListingState.UNAVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("bicycle");
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Porto");
        listing.setDropOffLocation("Lisboa");
        
        listing = listingRepository.save(listing);
        String listingId = listing.getId().toString();

        // Should still return the listing even if unavailable
        mockMvc.perform(get("/api/v1/renters/listings/{id}", listingId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(listingId))
            .andExpect(jsonPath("$.title").value("Unavailable Bike"))
            .andExpect(jsonPath("$.state").value("UNAVAILABLE"));
    }

    @Test
    @DisplayName("Integration test: Get listing by ID with invalid UUID format returns 400")
    @Requirement("VCT-35")
    void whenGetListingById_withInvalidUuidFormat_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/renters/listings/{id}", "invalid-uuid")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
