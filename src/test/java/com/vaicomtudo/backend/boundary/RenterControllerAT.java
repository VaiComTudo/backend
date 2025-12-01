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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

@SpringBootTest
@AutoConfigureMockMvc
class RenterControllerAT {

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

        Account account = new Account();
        account.setName("Test Owner");
        account.setEmail("owner@test.com");
        account.setPasswordHash("hashedpassword");

        owner = new User();
        owner.setAccount(account);
        owner.setBirthdate(LocalDate.of(1990, 1, 1));
        owner = userRepository.save(owner);
    }

    @Test
    @DisplayName("Acceptance test: As a renter, I can filter available items by category to view only the type of transportation I need")
    @Requirement("VCT-32")
    void asRenter_whenFilteringByCategory_thenViewOnlyMatchingTransportationType() throws Exception {
        // Given: There are multiple available listings with different categories
        Listing bicycle1 = createListing("Mountain Bike", "bicycle", ListingState.AVAILABLE, BigDecimal.valueOf(25.00));
        Listing bicycle2 = createListing("City Bike", "bicycle", ListingState.AVAILABLE, BigDecimal.valueOf(20.00));
        Listing scooter = createListing("Electric Scooter", "scooter", ListingState.AVAILABLE,
                BigDecimal.valueOf(30.00));
        Listing skate = createListing("Longboard", "skate", ListingState.AVAILABLE, BigDecimal.valueOf(15.00));
        Listing unavailableBicycle = createListing("Broken Bike", "bicycle", ListingState.UNAVAILABLE,
                BigDecimal.valueOf(10.00));

        listingRepository.save(bicycle1);
        listingRepository.save(bicycle2);
        listingRepository.save(scooter);
        listingRepository.save(skate);
        listingRepository.save(unavailableBicycle);

        // When: A renter filters by bicycle category
        // Then: Only available bicycles are returned
        mockMvc.perform(get("/api/v1/renters/listings")
                .param("category", "bicycle")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].vehicle.type").value("bicycle"))
                .andExpect(jsonPath("$[1].vehicle.type").value("bicycle"))
                .andExpect(jsonPath("$[0].state").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].state").value("AVAILABLE"))
                .andExpect(jsonPath("$[*].title")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("Mountain Bike", "City Bike")));

        // When: A renter filters by scooter category
        // Then: Only available scooters are returned
        mockMvc.perform(get("/api/v1/renters/listings")
                .param("category", "scooter")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vehicle.type").value("scooter"))
                .andExpect(jsonPath("$[0].title").value("Electric Scooter"))
                .andExpect(jsonPath("$[0].state").value("AVAILABLE"));

        // When: A renter filters by skate category
        // Then: Only available skates are returned
        mockMvc.perform(get("/api/v1/renters/listings")
                .param("category", "skate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vehicle.type").value("skate"))
                .andExpect(jsonPath("$[0].title").value("Longboard"))
                .andExpect(jsonPath("$[0].state").value("AVAILABLE"));
    }

    private Listing createListing(String title, String vehicleType, ListingState state, BigDecimal price) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle(title);
        listing.setDescription("Description for " + title);
        listing.setPrice(price);
        listing.setState(state);

        Vehicle vehicle = new Vehicle();
        vehicle.setType(vehicleType);
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Pickup Location");
        listing.setDropOffLocation("Dropoff Location");

        return listing;
    }
}
