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

import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.service.ListingService;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@SpringBootTest
@AutoConfigureMockMvc
class RenterControllerTest extends AbstractIntegrationTest {

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
        @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
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
        @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
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
        @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
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
        @DisplayName("GET /api/v1/renters/listings without category should return empty list")
        @Requirement("VCT-32")
        @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
        void whenGetListings_withoutCategory_thenReturnsEmptyList() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /api/v1/renters/listings?category= should return empty list")
        @Requirement("VCT-32")
        @WithMockUser(username = "test@email.com", roles = {"NORMAL_USER"})
        void whenGetListings_withEmptyCategory_thenReturnsEmptyList() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/v1/renters/listings")
                                .param("category", "")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(0)));
        }
}
