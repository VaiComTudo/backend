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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.data.entity.Account;
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
			Account account = new Account();
			account.setEmail("owner@test.com");
			owner.setAccount(account);

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
	void whenGetListingsByCategory_withBicycleCategory_thenReturnsBicycleListings() throws Exception {
		// Arrange
		List<Listing> bicycleListings = new ArrayList<>();
		bicycleListings.add(listing1);
		Page<Listing> bicyclePage = new PageImpl<>(bicycleListings, PageRequest.of(0, 20), bicycleListings.size());

		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.eq("bicycle"),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(bicyclePage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("category", "bicycle")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].title", is("Bicycle Rental")))
			.andExpect(jsonPath("$.content[0].vehicle.type", is("bicycle")))
			.andExpect(jsonPath("$.totalElements", is(1)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings?category=skate should return only skate listings")
	@Requirement("VCT-32")
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
		Page<Listing> skatePage = new PageImpl<>(skateListings, PageRequest.of(0, 20), skateListings.size());

		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.eq("skate"),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(skatePage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("category", "skate")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].title", is("Skate Rental")))
			.andExpect(jsonPath("$.content[0].vehicle.type", is("skate")))
			.andExpect(jsonPath("$.totalElements", is(1)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings?category=nonExistent should return empty list")
	@Requirement("VCT-32")
	void whenGetListingsByCategory_withNonExistentCategory_thenReturnsEmptyList() throws Exception {
		// Arrange
		Page<Listing> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);
		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.eq("nonExistent"),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(emptyPage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("category", "nonExistent")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(0)))
			.andExpect(jsonPath("$.totalElements", is(0)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings without category should return empty page")
	@Requirement("VCT-32")
	void whenGetListings_withoutCategory_thenReturnsEmptyPage() throws Exception {
		// Arrange
		Page<Listing> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);
		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(emptyPage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(0)))
			.andExpect(jsonPath("$.totalElements", is(0)))
			.andExpect(jsonPath("$.empty", is(true)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings?category= should return empty page")
	@Requirement("VCT-32")
	void whenGetListings_withEmptyCategory_thenReturnsEmptyPage() throws Exception {
		// Arrange
		Page<Listing> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);
		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.eq(""),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(emptyPage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("category", "")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(0)))
			.andExpect(jsonPath("$.totalElements", is(0)))
			.andExpect(jsonPath("$.empty", is(true)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings?location=Aveiro should return listings with matching location")
	@Requirement("VCT-33")
	void whenSearchListings_withLocation_thenReturnsMatchingListings() throws Exception {
		// Arrange
		List<Listing> locationListings = new ArrayList<>();
		locationListings.add(listing1);
		Page<Listing> locationPage = new PageImpl<>(locationListings, PageRequest.of(0, 20), locationListings.size());

		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.eq("Aveiro"),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(locationPage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("location", "Aveiro")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.totalElements", is(1)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings?minPrice=20&maxPrice=30 should return listings within price range")
	@Requirement("VCT-34")
	void whenSearchListings_withPriceRange_thenReturnsMatchingListings() throws Exception {
		// Arrange
		List<Listing> priceRangeListings = new ArrayList<>();
		priceRangeListings.add(listing1);
		Page<Listing> pricePage = new PageImpl<>(priceRangeListings, PageRequest.of(0, 20), priceRangeListings.size());

		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.eq(java.math.BigDecimal.valueOf(20)),
			org.mockito.ArgumentMatchers.eq(java.math.BigDecimal.valueOf(30)),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(pricePage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("minPrice", "20")
			.param("maxPrice", "30")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.totalElements", is(1)));
	}

    @Test
    @DisplayName("GET /api/v1/renters/listings should not return own listings when authenticated")
    @Requirement("VCT-32")
    void whenAuthenticatedUser_thenOwnListingsAreExcluded() throws Exception {
        // Arrange
        Page<Listing> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);
        when(listingService.searchAvailableListings(
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq("owner@test.com"),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(emptyPage);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/renters/listings")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner@test.com"))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(0)));
    }
    @Test
    @DisplayName("GET /api/v1/renters/listings should return all listings when unauthenticated")
    @Requirement("VCT-32")
    void whenUnauthenticatedUser_thenAllListingsAreReturned() throws Exception {
        // Arrange
        List<Listing> listings = new ArrayList<>();
        listings.add(listing1);
        Page<Listing> page = new PageImpl<>(listings, PageRequest.of(0, 20), listings.size());
        when(listingService.searchAvailableListings(
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(page);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/renters/listings")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.totalElements", is(1)));
    }
	@Test
	@DisplayName("GET /api/v1/renters/listings with combined filters should return matching listings")
	@Requirement("VCT-33")
	void whenSearchListings_withCombinedFilters_thenReturnsMatchingListings() throws Exception {
		// Arrange
		List<Listing> combinedListings = new ArrayList<>();
		combinedListings.add(listing1);
		Page<Listing> combinedPage = new PageImpl<>(combinedListings, PageRequest.of(0, 20), combinedListings.size());

		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.eq("bicycle"),
			org.mockito.ArgumentMatchers.eq("Aveiro"),
			org.mockito.ArgumentMatchers.eq(java.math.BigDecimal.valueOf(20)),
			org.mockito.ArgumentMatchers.eq(java.math.BigDecimal.valueOf(30)),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(combinedPage);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("category", "bicycle")
			.param("location", "Aveiro")
			.param("minPrice", "20")
			.param("maxPrice", "30")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].title", is("Bicycle Rental")))
			.andExpect(jsonPath("$.totalElements", is(1)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings?category=bicycle should exclude own listings when authenticated")
	@Requirement("VCT-33")
	void whenAuthenticatedUserSearchesByCategory_thenOwnListingsAreExcluded() throws Exception {
		// Arrange
		User otherOwner = new User();
		otherOwner.setId(UUID.randomUUID());
		Account otherAccount = new Account();
		otherAccount.setEmail("other@test.com");
		otherOwner.setAccount(otherAccount);

		Listing otherListing = new Listing();
		otherListing.setId(UUID.randomUUID());
		otherListing.setOwner(otherOwner);
		otherListing.setTitle("Other User's Bicycle");
		otherListing.setState(ListingState.AVAILABLE);
		otherListing.setPrice(BigDecimal.valueOf(30.00));

		Vehicle vehicle = new Vehicle();
		vehicle.setType("bicycle");
		vehicle.setCondition(VehicleCondition.GOOD);
		otherListing.setVehicle(vehicle);

		List<Listing> listings = new ArrayList<>();
		listings.add(otherListing);
		Page<Listing> page = new PageImpl<>(listings, PageRequest.of(0, 20), listings.size());

		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.eq("bicycle"),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.eq("owner@test.com"),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("category", "bicycle")
			.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner@test.com"))
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].title", is("Other User's Bicycle")))
			.andExpect(jsonPath("$.totalElements", is(1)));
	}

	@Test
	@DisplayName("GET /api/v1/renters/listings with combined filters should exclude own listings when authenticated")
	@Requirement("VCT-34")
	void whenAuthenticatedUserSearchesWithCombinedFilters_thenOwnListingsAreExcluded() throws Exception {
		// Arrange
		User otherOwner = new User();
		otherOwner.setId(UUID.randomUUID());
		Account otherAccount = new Account();
		otherAccount.setEmail("other@test.com");
		otherOwner.setAccount(otherAccount);

		Listing otherListing = new Listing();
		otherListing.setId(UUID.randomUUID());
		otherListing.setOwner(otherOwner);
		otherListing.setTitle("Other User's Bicycle");
		otherListing.setState(ListingState.AVAILABLE);
		otherListing.setPrice(BigDecimal.valueOf(25.00));
		otherListing.setPickUpLocation("Aveiro");
		otherListing.setDropOffLocation("Porto");

		Vehicle vehicle = new Vehicle();
		vehicle.setType("bicycle");
		vehicle.setCondition(VehicleCondition.GOOD);
		otherListing.setVehicle(vehicle);

		List<Listing> listings = new ArrayList<>();
		listings.add(otherListing);
		Page<Listing> page = new PageImpl<>(listings, PageRequest.of(0, 20), listings.size());

		when(listingService.searchAvailableListings(
			org.mockito.ArgumentMatchers.eq("bicycle"),
			org.mockito.ArgumentMatchers.eq("Aveiro"),
			org.mockito.ArgumentMatchers.eq(java.math.BigDecimal.valueOf(20)),
			org.mockito.ArgumentMatchers.eq(java.math.BigDecimal.valueOf(30)),
			org.mockito.ArgumentMatchers.eq("owner@test.com"),
			org.mockito.ArgumentMatchers.any()))
			.thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/v1/renters/listings")
			.param("category", "bicycle")
			.param("location", "Aveiro")
			.param("minPrice", "20")
			.param("maxPrice", "30")
			.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner@test.com"))
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].title", is("Other User's Bicycle")))
			.andExpect(jsonPath("$.totalElements", is(1)));
	}
}
