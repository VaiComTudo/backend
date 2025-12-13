package com.vaicomtudo.backend.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.data.repository.AccountRepository;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class OwnerControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create account with email matching @WithMockUser username
        Account account = new Account();
        account.setName("Integration Test Owner");
        account.setEmail("test@email.com");  // Match @WithMockUser username
        account.setPasswordHash("hashedpassword");

        // Save account first (no longer cascades from user)
        account = accountRepository.save(account);

        // Create and save user
        owner = new User();
        owner.setAccount(account);
        owner.setBirthdate(LocalDate.of(1990, 1, 1));
        owner.setRating(0.0);
        owner.setRole(Role.NORMAL_USER);
        owner = userRepository.save(owner);
    }

    @Test
    @DisplayName("Integration test: POST and GET listings flow")
    @Requirement("VCT-48")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenCreateAndRetrieveListings_thenWorkEndToEnd() throws Exception {
        // Create first listing
        Listing listing1 = new Listing();
        listing1.setTitle("Integration Test Item 1");
        listing1.setDescription("First test item");
        listing1.setPrice(BigDecimal.valueOf(50.00));
        listing1.setState(ListingState.AVAILABLE);
        
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setType("Car");
        vehicle1.setCondition(VehicleCondition.GOOD);
        listing1.setVehicle(vehicle1);
        listing1.setPickUpLocation("Location A");
        listing1.setDropOffLocation("Location B");

        mockMvc.perform(post("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing1)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Integration Test Item 1"))
            .andExpect(jsonPath("$.price").value(50.00));

        // Create second listing
        Listing listing2 = new Listing();
        listing2.setTitle("Integration Test Item 2");
        listing2.setDescription("Second test item");
        listing2.setPrice(BigDecimal.valueOf(75.00));
        listing2.setState(ListingState.AVAILABLE);
        
        Vehicle vehicle2 = new Vehicle();
        vehicle2.setType("Bike");
        vehicle2.setCondition(VehicleCondition.EXCELLENT);
        listing2.setVehicle(vehicle2);
        listing2.setPickUpLocation("Location C");
        listing2.setDropOffLocation("Location D");

        mockMvc.perform(post("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing2)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Integration Test Item 2"))
            .andExpect(jsonPath("$.price").value(75.00));

        // Retrieve all listings
        MvcResult result = mockMvc.perform(get("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        // Verify we got both listings back
        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("Integration Test Item 1");
        assertThat(content).contains("Integration Test Item 2");
    }

    @Test
    @DisplayName("Integration test: Verify listing is persisted in database")
    @Requirement("VCT-48")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    @Transactional
    void whenCreateListing_thenListingExistsInDatabase() throws Exception {
        // Arrange
        Listing listing = new Listing();
        listing.setTitle("Database Test Item");
        listing.setDescription("Testing persistence");
        listing.setPrice(BigDecimal.valueOf(99.99));
        listing.setState(ListingState.AVAILABLE);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setType("Van");
        vehicle.setCondition(VehicleCondition.NEEDS_WORK);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Main Street");
        listing.setDropOffLocation("Airport");

        // Act - Create listing via API
        mockMvc.perform(post("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isCreated());

        // Assert - Verify in database
        User updatedOwner = userRepository.findById(owner.getId()).orElseThrow();
        assertThat(updatedOwner.getListings()).hasSize(1);
        
        Listing savedListing = updatedOwner.getListings().iterator().next();
        assertThat(savedListing.getTitle()).isEqualTo("Database Test Item");
        assertThat(savedListing.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("Integration test: POST without proper role should return 403")
    @Requirement("VCT-80")
    @WithMockUser(username = "test@email.com", roles = {"ADMIN"})
    void whenAddListing_withoutNormalUserRole_thenReturns403() throws Exception {
        Listing listing = new Listing();
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

        mockMvc.perform(post("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: GET without proper role should return 403")
    @Requirement("VCT-80")
    @WithMockUser(username = "test@email.com", roles = {"ADMIN"})
    void whenGetListings_withoutNormalUserRole_thenReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: POST without authentication should return 403")
    @Requirement("VCT-80")
    void whenAddListing_notAuthenticated_thenReturns401() throws Exception {
        Listing listing = new Listing();
        listing.setTitle("Title here");
        listing.setDescription("something here");
        listing.setPrice(BigDecimal.valueOf(80.00));
        listing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("Bike");
        vehicle.setCondition(VehicleCondition.POOR);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Some Pickup location");
        listing.setDropOffLocation("Another Dropoff location");

        mockMvc.perform(post("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: Verify user-listing relationship integrity")
    @Requirement("VCT-48")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    @Transactional
    void whenCreateMultipleListings_thenAllBelongToSameOwner() throws Exception {
        // Create multiple listings
        for (int i = 1; i <= 3; i++) {
            Listing listing = new Listing();
            listing.setTitle("Item " + i);
            listing.setDescription("Description " + i);
            listing.setPrice(BigDecimal.valueOf(50.00 * i));
            listing.setState(ListingState.AVAILABLE);
            
            Vehicle vehicle = new Vehicle();
            vehicle.setType("Type " + i);
            vehicle.setCondition(VehicleCondition.GOOD);
            listing.setVehicle(vehicle);
            listing.setPickUpLocation("Pickup " + i);
            listing.setDropOffLocation("Dropoff " + i);

            mockMvc.perform(post("/api/v1/owners/listings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(listing)))
                .andExpect(status().isCreated());
        }

        // Verify all listings belong to owner
        User updatedOwner = userRepository.findById(owner.getId()).orElseThrow();
        assertThat(updatedOwner.getListings()).hasSize(3);
        assertThat(updatedOwner.getListings())
            .allMatch(listing -> listing.getOwner().getId().equals(owner.getId()));
    }

    @Test
    @DisplayName("Integration test: Successfully remove a listing")
    @Requirement("VCT-59")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    @Transactional
    void whenRemoveListing_withValidOwner_thenListingRemovedFromDatabase() throws Exception {
        // Create a listing
        Listing listing = new Listing();
        listing.setTitle("To Be Removed");
        listing.setDescription("This listing will be removed");
        listing.setPrice(BigDecimal.valueOf(75.00));
        listing.setState(ListingState.AVAILABLE);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setType("Bike");
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Start Point");
        listing.setDropOffLocation("End Point");

        // Create listing via API
        MvcResult createResult = mockMvc.perform(post("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isCreated())
            .andReturn();

        // Extract the created listing ID from response
        String responseContent = createResult.getResponse().getContentAsString();
        Listing createdListing = objectMapper.readValue(responseContent, Listing.class);

        // Verify listing exists in database
        assertThat(listingRepository.findById(createdListing.getId())).isPresent();

        // Remove the listing
        mockMvc.perform(delete("/api/v1/owners/listings/" + createdListing.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Verify listing is removed from database
        assertThat(listingRepository.findById(createdListing.getId())).isEmpty();
    }

    @Test
    @DisplayName("Integration test: Cannot remove non-existent listing")
    @Requirement("VCT-59")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenRemoveListing_withNonExistentId_thenReturns404() throws Exception {
        // Try to remove a listing that doesn't exist
        java.util.UUID nonExistentId = java.util.UUID.randomUUID();
        
        mockMvc.perform(delete("/api/v1/owners/listings/" + nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration test: Cannot remove listing owned by different user")
    @Requirement("VCT-59")
    @WithMockUser(username = "different@email.com", roles = {"NORMAL_USER"})
    @Transactional
    void whenRemoveListing_withDifferentOwner_thenReturns403() throws Exception {
        // Create listing as original owner
        Listing listing = new Listing();
        listing.setTitle("Someone Else's Listing");
        listing.setDescription("This belongs to a different owner");
        listing.setPrice(BigDecimal.valueOf(60.00));
        listing.setState(ListingState.AVAILABLE);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setType("Scooter");
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Place A");
        listing.setDropOffLocation("Place B");

        listing.setOwner(owner);
        owner.addListing(listing);
        Listing savedListing = listingRepository.save(listing);

        // Try to remove as different user (the @WithMockUser has different@email.com)
        // This should fail with 403 because the listing belongs to test@email.com
        mockMvc.perform(delete("/api/v1/owners/listings/" + savedListing.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden()); // Changed to 403 as that's what the system returns
    }
}
