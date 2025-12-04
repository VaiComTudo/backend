package com.vaicomtudo.backend.boundary;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.service.ListingService;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@SpringBootTest
@AutoConfigureMockMvc
class RenterControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ListingService listingService;

        private User owner;
        private Listing listing1;

        @BeforeEach
        void setUp() {
                owner = new User();
                owner.setId(UUID.randomUUID());

                listing1 = new Listing();
                listing1.setId(UUID.randomUUID());
                listing1.setOwner(owner);
                listing1.setTitle("Bicycle Rental");
                listing1.setDescription("Mountain bike for rent");
                listing1.setPrice(BigDecimal.valueOf(25.00));
                listing1.setState(ListingState.AVAILABLE);

                Vehicle vehicle1 = new Vehicle();
                vehicle1.setType("bicycle");
                vehicle1.setCondition(VehicleCondition.GOOD);
                listing1.setVehicle(vehicle1);
                listing1.setPickUpLocation("Location A");
                listing1.setDropOffLocation("Location B");
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?category=bicycle should return only bicycle listings")
        @Requirement("VCT-32")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListingsByCategory_withBicycleCategory_thenReturnsBicycleListings() throws Exception {
                // Arrange
                List<Listing> bicycleListings = new ArrayList<>();
                bicycleListings.add(listing1);

                when(listingService.getAvailableListingsByCategory("bicycle"))
                                .thenReturn(bicycleListings);

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("category", "bicycle")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("Bicycle Rental")))
                                .andExpect(jsonPath("$[0].vehicle.type", is("bicycle")));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?category=skate should return only skate listings")
        @Requirement("VCT-32")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListingsByCategory_withSkateCategory_thenReturnsSkateListings() throws Exception {
                // Arrange
                Listing skateListing = new Listing();
                skateListing.setId(UUID.randomUUID());
                skateListing.setOwner(owner);
                skateListing.setTitle("Skate Rental");
                skateListing.setDescription("Longboard skate");
                skateListing.setPrice(BigDecimal.valueOf(20.00));
                skateListing.setState(ListingState.AVAILABLE);

                Vehicle vehicle = new Vehicle();
                vehicle.setType("skate");
                vehicle.setCondition(VehicleCondition.GOOD);
                skateListing.setVehicle(vehicle);
                skateListing.setPickUpLocation("Location E");
                skateListing.setDropOffLocation("Location F");

                List<Listing> skateListings = new ArrayList<>();
                skateListings.add(skateListing);

                when(listingService.getAvailableListingsByCategory("skate"))
                                .thenReturn(skateListings);

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("category", "skate")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("Skate Rental")))
                                .andExpect(jsonPath("$[0].vehicle.type", is("skate")));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?category=nonExistent should return empty list")
        @Requirement("VCT-32")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListingsByCategory_withNonExistentCategory_thenReturnsEmptyList() throws Exception {
                // Arrange
                when(listingService.getAvailableListingsByCategory("nonExistent"))
                                .thenReturn(new ArrayList<>());

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("category", "nonExistent")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings without category should return all available listings")
        @Requirement("VCT-32")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListings_withoutCategory_thenReturnsAllListings() throws Exception {
                // Arrange
                List<Listing> allListings = new ArrayList<>();
                allListings.add(listing1);

                when(listingService.getAllAvailableListings())
                                .thenReturn(allListings);

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?category= should return all available listings")
        @Requirement("VCT-32")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListings_withEmptyCategory_thenReturnsAllListings() throws Exception {
                // Arrange
                List<Listing> allListings = new ArrayList<>();
                allListings.add(listing1);

                when(listingService.getAllAvailableListings())
                                .thenReturn(allListings);

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("category", "")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?location=Lisboa should return listings in Lisboa")
        @Requirement("VCT-33")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListingsByLocation_withLisboa_thenReturnsLisboaListings() throws Exception {
                // Arrange
                Listing lisboaListing = new Listing();
                lisboaListing.setId(UUID.randomUUID());
                lisboaListing.setOwner(owner);
                lisboaListing.setTitle("Bike in Lisboa");
                lisboaListing.setDescription("Bike rental in Lisboa");
                lisboaListing.setPrice(BigDecimal.valueOf(20.00));
                lisboaListing.setState(ListingState.AVAILABLE);

                Vehicle vehicle = new Vehicle();
                vehicle.setType("bicycle");
                vehicle.setCondition(VehicleCondition.GOOD);
                lisboaListing.setVehicle(vehicle);
                lisboaListing.setPickUpLocation("Lisboa");
                lisboaListing.setDropOffLocation("Porto");

                List<Listing> lisboaListings = new ArrayList<>();
                lisboaListings.add(lisboaListing);

                when(listingService.getAvailableListingsByLocation("Lisboa"))
                                .thenReturn(lisboaListings);

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("location", "Lisboa")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("Bike in Lisboa")))
                                .andExpect(jsonPath("$[0].pickUpLocation", is("Lisboa")));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?category=bicycle&location=Lisboa should return bicycle listings in Lisboa")
        @Requirement("VCT-33")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListingsByCategoryAndLocation_thenReturnsFilteredListings() throws Exception {
                // Arrange
                Listing lisboaBikeListing = new Listing();
                lisboaBikeListing.setId(UUID.randomUUID());
                lisboaBikeListing.setOwner(owner);
                lisboaBikeListing.setTitle("Bike in Lisboa");
                lisboaBikeListing.setDescription("Bike rental in Lisboa");
                lisboaBikeListing.setPrice(BigDecimal.valueOf(20.00));
                lisboaBikeListing.setState(ListingState.AVAILABLE);

                Vehicle vehicle = new Vehicle();
                vehicle.setType("bicycle");
                vehicle.setCondition(VehicleCondition.GOOD);
                lisboaBikeListing.setVehicle(vehicle);
                lisboaBikeListing.setPickUpLocation("Lisboa");
                lisboaBikeListing.setDropOffLocation("Porto");

                List<Listing> filteredListings = new ArrayList<>();
                filteredListings.add(lisboaBikeListing);

                when(listingService.getAvailableListingsByCategoryAndLocation("bicycle", "Lisboa"))
                                .thenReturn(filteredListings);

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("category", "bicycle")
                                .param("location", "Lisboa")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("Bike in Lisboa")))
                                .andExpect(jsonPath("$[0].vehicle.type", is("bicycle")))
                                .andExpect(jsonPath("$[0].pickUpLocation", is("Lisboa")));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?location=nonExistent should return empty list")
        @Requirement("VCT-33")
        @WithMockUser(username = "test@email.com", roles = { "NORMAL_USER" })
        void whenGetListingsByLocation_withNonExistentLocation_thenReturnsEmptyList() throws Exception {
                // Arrange
                when(listingService.getAvailableListingsByLocation("nonExistent"))
                                .thenReturn(new ArrayList<>());

                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("location", "nonExistent")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(0)));
        }
}
