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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.exception.EmailNotFoundException;
import com.vaicomtudo.backend.exception.MismatchEmailException;
import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.service.ListingService;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @Autowired
    private ObjectMapper objectMapper;

    private User owner;
    private Listing listing;
    private String ownerEmail;

    @BeforeEach
    void setUp() {
        ownerEmail = "test@email.com";

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setEmail(ownerEmail);

        owner = new User();
        owner.setAccount(account);
        owner.setId(account.getId());

        listing = new Listing();
        listing.setId(UUID.randomUUID());
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
    @DisplayName("POST /api/v1/owners/listings should create listing successfully")
    @Requirement("VCT-48")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenAddListing_withValidData_thenReturns201() throws Exception {
        // Arrange
        when(listingService.saveListing(any(Listing.class), eq(ownerEmail)))
            .thenReturn(listing);

        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/listings")
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
    @DisplayName("POST /api/v1/owners/listings should return 400 when email mismatch")
    @Requirement("VCT-48")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenAddListing_withMismatchedEmail_thenReturns400() throws Exception {
        // Arrange
        when(listingService.saveListing(any(Listing.class), eq(ownerEmail)))
            .thenThrow(new MismatchEmailException());

        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/listings", ownerEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/owners/listings should return 400 when user not found")
    @Requirement("VCT-48")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenAddListing_withNonExistentUser_thenReturns400() throws Exception {
        // Arrange
        when(listingService.saveListing(any(Listing.class), eq(ownerEmail)))
            .thenThrow(new EmailNotFoundException());

        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/listings", ownerEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listing)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/owners/listings should return 400 with invalid JSON")
    @Requirement("VCT-48")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenAddListing_withInvalidJson_thenReturns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/owners/listings", ownerEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/owners/listings should return paginated listings with default parameters")
    @Requirement("VCT-47")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenGetListings_withDefaultPagination_thenReturnsPaginatedListings() throws Exception {
        // Arrange
        Listing listing2 = new Listing();
        listing2.setId(UUID.randomUUID());
        listing2.setOwner(owner);
        listing2.setTitle("Another Item");
        listing2.setDescription("Another Description");
        listing2.setPrice(BigDecimal.valueOf(75.00));
        listing2.setState(ListingState.AVAILABLE);

        List<Listing> listingsList = new ArrayList<>();
        listingsList.add(listing);
        listingsList.add(listing2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Listing> page = new PageImpl<>(listingsList, pageable, listingsList.size());

        when(listingService.getListingsPaginated(eq(ownerEmail), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements", is(2)))
            .andExpect(jsonPath("$.totalPages", is(1)))
            .andExpect(jsonPath("$.size", is(10)))
            .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/owners/listings should return paginated listings with custom parameters")
    @Requirement("VCT-47")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenGetListings_withCustomPagination_thenReturnsPaginatedListings() throws Exception {
        // Arrange
        List<Listing> listingsList = new ArrayList<>();
        listingsList.add(listing);

        Pageable pageable = PageRequest.of(0, 5);
        Page<Listing> page = new PageImpl<>(listingsList, pageable, 10);

        when(listingService.getListingsPaginated(eq(ownerEmail), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/owners/listings")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "price")
                .param("sortDirection", "desc")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.totalElements", is(10)))
            .andExpect(jsonPath("$.totalPages", is(2)))
            .andExpect(jsonPath("$.size", is(5)))
            .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/owners/listings should return empty page when no listings")
    @Requirement("VCT-47")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenGetListings_withNoListings_thenReturnsEmptyPage() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Listing> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);

        when(listingService.getListingsPaginated(eq(ownerEmail), any(Pageable.class))).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(0)))
            .andExpect(jsonPath("$.totalPages", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/owners/listings should return 404 when user not found")
    @Requirement("VCT-47")
    @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
    void whenGetListings_withNonExistentUser_thenReturns404() throws Exception {
        // Arrange
        when(listingService.getListingsPaginated(eq(ownerEmail), any(Pageable.class)))
            .thenThrow(new EmailNotFoundException());

        // Act & Assert
        mockMvc.perform(get("/api/v1/owners/listings")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

}
