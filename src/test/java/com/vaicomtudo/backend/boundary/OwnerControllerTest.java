package com.vaicomtudo.backend.boundary;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.exception.IDNotFoundException;
import com.vaicomtudo.backend.exception.MismatchIDException;
import com.vaicomtudo.backend.service.ListingService;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@WebMvcTest(OwnerController.class)
@AutoConfigureMockMvc(addFilters = false)
class OwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @Autowired
    private ObjectMapper objectMapper;

    private User owner;
    private Listing listing;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = new User();
        owner.setId(ownerId);

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
    @DisplayName("POST /api/v1/owners/{id}/listings should create listing successfully")
    @Requirement("VCT-48")
    void whenAddListing_withValidData_thenReturns201() throws Exception {
        // Arrange
        when(listingService.saveListing(any(Listing.class), eq(ownerId)))
            .thenReturn(listing);

        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/{id}/listings", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title", is("Test Item")))
            .andExpect(jsonPath("$.description", is("Test Description")))
            .andExpect(jsonPath("$.price", is(50.00)))
            .andExpect(jsonPath("$.state", is("AVAILABLE")));
    }

    @Test
    @DisplayName("POST /api/v1/owners/{id}/listings should return 400 when ID mismatch")
    @Requirement("VCT-48")
    void whenAddListing_withMismatchedId_thenReturns400() throws Exception {
        // Arrange
        when(listingService.saveListing(any(Listing.class), eq(ownerId)))
            .thenThrow(new MismatchIDException());

        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/{id}/listings", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/owners/{id}/listings should return 400 when user not found")
    @Requirement("VCT-48")
    void whenAddListing_withNonExistentUser_thenReturns400() throws Exception {
        // Arrange
        when(listingService.saveListing(any(Listing.class), eq(ownerId)))
            .thenThrow(new IDNotFoundException());

        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/{id}/listings", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/owners/{id}/listings should return 400 with invalid JSON")
    @Requirement("VCT-48")
    void whenAddListing_withInvalidJson_thenReturns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/{id}/listings", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/owners/{id}/listings should return all listings")
    @Requirement("VCT-48")
    void whenGetListings_withExistingUser_thenReturnsListings() throws Exception {
        // Arrange
        Listing listing2 = new Listing();
        listing2.setId(UUID.randomUUID());
        listing2.setOwner(owner);
        listing2.setTitle("Another Item");
        listing2.setDescription("Another Description");
        listing2.setPrice(BigDecimal.valueOf(75.00));
        listing2.setState(ListingState.AVAILABLE);

        Set<Listing> listings = new HashSet<>();
        listings.add(listing);
        listings.add(listing2);

        when(listingService.getListings(ownerId)).thenReturn(listings);

        // Act & Assert
        mockMvc.perform(get("/api/v1/owners/{id}/listings", ownerId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/owners/{id}/listings should return empty array when no listings")
    @Requirement("VCT-48")
    void whenGetListings_withNoListings_thenReturnsEmptyArray() throws Exception {
        // Arrange
        when(listingService.getListings(ownerId)).thenReturn(new HashSet<>());

        // Act & Assert
        mockMvc.perform(get("/api/v1/owners/{id}/listings", ownerId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/owners/{id}/listings should return 400 when user not found")
    @Requirement("VCT-48")
    void whenGetListings_withNonExistentUser_thenReturns400() throws Exception {
        // Arrange
        when(listingService.getListings(ownerId))
            .thenThrow(new IDNotFoundException());

        // Act & Assert
        mockMvc.perform(get("/api/v1/owners/{id}/listings", ownerId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

}
