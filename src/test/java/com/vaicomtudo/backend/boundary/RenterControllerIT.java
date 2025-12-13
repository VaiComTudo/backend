package com.vaicomtudo.backend.boundary;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MvcResult;

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
    @WithMockUser(username = "renter@test.com", roles = {"NORMAL_USER"})
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
        MvcResult result = mockMvc.perform(get("/api/v1/renters/listings")
                .param("category", "bicycle")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Verify only bicycle is returned
        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("Mountain Bike").contains("bicycle").doesNotContain("Electric Scooter").doesNotContain("scooter");
    }

    @Test
    @DisplayName("Integration test: Only available listings are returned")
    @Requirement("VCT-32")
    @WithMockUser(username = "renter@test.com", roles = {"NORMAL_USER"})
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
        MvcResult result = mockMvc.perform(get("/api/v1/renters/listings")
                .param("category", "bicycle")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Verify only available listing is returned
        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("Available Bike").doesNotContain("Unavailable Bike");
    }

    @Test
    @DisplayName("Integration test: Multiple listings of same category are returned")
    @Requirement("VCT-32")
    @WithMockUser(username = "renter@test.com", roles = {"NORMAL_USER"})
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
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].vehicle.type").value("bicycle"))
                .andExpect(jsonPath("$[1].vehicle.type").value("bicycle"))
                .andExpect(jsonPath("$[2].vehicle.type").value("bicycle"));
    }
}
