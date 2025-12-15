package com.vaicomtudo.backend.boundary;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Booking;
import com.vaicomtudo.backend.data.entity.BookingState;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.BookingRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.dto.BookingRequest;
import com.vaicomtudo.backend.dto.BookingStateUpdateRequest;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class BookingControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User renter;
    private User owner;
    private Listing listing;
    private String renterEmail = "renter@test.com";
    private String ownerEmail = "owner@test.com";

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        // Create renter
        Account renterAccount = new Account();
        renterAccount.setName("Renter User");
        renterAccount.setEmail(renterEmail);
        renterAccount.setPasswordHash("hashedpassword");
        renterAccount = accountRepository.save(renterAccount);

        renter = new User();
        renter.setAccount(renterAccount);
        renter.setBirthdate(LocalDate.of(1995, 5, 15));
        renter = userRepository.save(renter);

        // Create owner
        Account ownerAccount = new Account();
        ownerAccount.setName("Owner User");
        ownerAccount.setEmail(ownerEmail);
        ownerAccount.setPasswordHash("hashedpassword");
        ownerAccount = accountRepository.save(ownerAccount);

        owner = new User();
        owner.setAccount(ownerAccount);
        owner.setBirthdate(LocalDate.of(1990, 1, 1));
        owner = userRepository.save(owner);

        // Create listing
        listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Electric Scooter");
        listing.setDescription("Fast electric scooter for weekend activities");
        listing.setPrice(BigDecimal.valueOf(25.00));
        listing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("scooter");
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Downtown");
        listing.setDropOffLocation("Downtown");
        listing = listingRepository.save(listing);
    }

    @Test
    @DisplayName("Integration test: Renter creates booking request")
    @Requirement("VCT-36")
    @WithMockUser(username = "renter@test.com")
    void whenRenterCreatesBooking_thenBookingCreated() throws Exception {
        // Arrange
        LocalDateTime pickupDate = LocalDateTime.now().plusDays(7);
        LocalDateTime dropoffDate = LocalDateTime.now().plusDays(9);

        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.listingId").value(listing.getId().toString()))
            .andExpect(jsonPath("$.listingTitle").value("Electric Scooter"))
            .andExpect(jsonPath("$.renterId").value(renter.getId().toString()))
            .andExpect(jsonPath("$.renterName").value("Renter User"))
            .andExpect(jsonPath("$.ownerId").value(owner.getId().toString()))
            .andExpect(jsonPath("$.ownerName").value("Owner User"))
            .andExpect(jsonPath("$.state").value("REQUESTED"));
    }

    @Test
    @DisplayName("Integration test: Booking rejected when dropoff before pickup")
    @Requirement("VCT-36")
    @WithMockUser(username = "renter@test.com")
    void whenCreateBooking_withInvalidDates_thenBadRequest() throws Exception {
        // Arrange
        LocalDateTime pickupDate = LocalDateTime.now().plusDays(9);
        LocalDateTime dropoffDate = LocalDateTime.now().plusDays(7);

        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration test: Renter cannot book own listing")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenRenterBooksOwnListing_thenForbidden() throws Exception {
        // Arrange
        LocalDateTime pickupDate = LocalDateTime.now().plusDays(7);
        LocalDateTime dropoffDate = LocalDateTime.now().plusDays(9);

        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: Renter gets their bookings")
    @Requirement("VCT-36")
    @WithMockUser(username = "renter@test.com")
    void whenRenterGetsBookings_thenReturnsList() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        bookingRepository.save(booking);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bookings/renter")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].renterId").value(renter.getId().toString()))
            .andExpect(jsonPath("$[0].state").value("REQUESTED"));
    }

    @Test
    @DisplayName("Integration test: Owner gets their bookings")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenOwnerGetsBookings_thenReturnsList() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        bookingRepository.save(booking);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bookings/owner")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].ownerId").value(owner.getId().toString()))
            .andExpect(jsonPath("$[0].state").value("REQUESTED"));
    }

    @Test
    @DisplayName("Integration test: Get specific booking as renter")
    @Requirement("VCT-36")
    @WithMockUser(username = "renter@test.com")
    void whenGetBookingAsRenter_thenReturnBooking() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        booking = bookingRepository.save(booking);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bookings/" + booking.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(booking.getId().toString()))
            .andExpect(jsonPath("$.state").value("REQUESTED"));
    }

    @Test
    @DisplayName("Integration test: Owner accepts booking request")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenOwnerAcceptsBooking_thenBookingAccepted() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        booking = bookingRepository.save(booking);

        BookingStateUpdateRequest updateRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.ACCEPTED)
            .build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("ACCEPTED"));
    }

    @Test
    @DisplayName("Integration test: Owner denies booking request")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenOwnerDeniesBooking_thenBookingDenied() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        booking = bookingRepository.save(booking);

        BookingStateUpdateRequest updateRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.DENIED)
            .build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("DENIED"));
    }

    @Test
    @DisplayName("Integration test: Renter cannot accept booking")
    @Requirement("VCT-36")
    @WithMockUser(username = "renter@test.com")
    void whenRenterTriesToAccept_thenForbidden() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        booking = bookingRepository.save(booking);

        BookingStateUpdateRequest updateRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.ACCEPTED)
            .build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: Owner starts accepted booking")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenOwnerStartsAcceptedBooking_thenBookingInProgress() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.ACCEPTED);
        booking = bookingRepository.save(booking);

        BookingStateUpdateRequest updateRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.IN_PROGRESS)
            .build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Integration test: Cannot start booking from REQUESTED state")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenStartingFromRequested_thenBadRequest() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        booking = bookingRepository.save(booking);

        BookingStateUpdateRequest updateRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.IN_PROGRESS)
            .build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration test: Owner marks booking as returned")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenOwnerMarksAsReturned_thenBookingReturned() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().minusDays(2));
        booking.setDropoffDateTime(LocalDateTime.now().minusDays(1));
        booking.setState(BookingState.IN_PROGRESS);
        booking = bookingRepository.save(booking);

        BookingStateUpdateRequest updateRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.RETURNED)
            .build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("RETURNED"));
    }

    @Test
    @DisplayName("Integration test: Owner reports booking as stolen")
    @Requirement("VCT-36")
    @WithMockUser(username = "owner@test.com")
    void whenOwnerReportsStolen_thenBookingStolen() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().minusDays(2));
        booking.setDropoffDateTime(LocalDateTime.now().minusDays(1));
        booking.setState(BookingState.IN_PROGRESS);
        booking = bookingRepository.save(booking);

        BookingStateUpdateRequest updateRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.STOLEN)
            .build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("STOLEN"));
    }

    @Test
    @DisplayName("Integration test: Renter cancels booking request")
    @Requirement("VCT-36")
    @WithMockUser(username = "renter@test.com")
    void whenRenterCancelsBooking_thenBookingCancelled() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        booking = bookingRepository.save(booking);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/bookings/" + booking.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Verify booking is cancelled
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getState()).isEqualTo(BookingState.CANCELLED);
    }

    @Test
    @DisplayName("Integration test: Owner can also cancel booking")
    @Requirement("VCT-XX")
    @WithMockUser(username = "owner@test.com")
    void whenOwnerCancelsBooking_thenBookingCancelled() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.REQUESTED);
        booking = bookingRepository.save(booking);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/bookings/" + booking.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Verify booking is cancelled
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getState()).isEqualTo(BookingState.CANCELLED);
    }

    @Test
    @DisplayName("Integration test: Can cancel accepted booking")
    @Requirement("VCT-XX")
    @WithMockUser(username = "renter@test.com")
    void whenCancelAcceptedBooking_thenBookingCancelled() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(LocalDateTime.now().plusDays(7));
        booking.setDropoffDateTime(LocalDateTime.now().plusDays(9));
        booking.setState(BookingState.ACCEPTED);
        booking = bookingRepository.save(booking);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/bookings/" + booking.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Verify booking is cancelled
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getState()).isEqualTo(BookingState.CANCELLED);
    }

    @Test
    @DisplayName("Integration test: Full booking workflow from request to return")
    @Requirement("VCT-36")
    void whenFullBookingWorkflow_thenAllStatesWork() throws Exception {
        // Step 1: Renter creates booking
        LocalDateTime pickupDate = LocalDateTime.now().plusDays(7);
        LocalDateTime dropoffDate = LocalDateTime.now().plusDays(9);

        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        String bookingIdString = mockMvc.perform(post("/api/v1/bookings")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(renterEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.state").value("REQUESTED"))
            .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(bookingIdString).get("id").asString();

        // Step 2: Owner accepts booking
        BookingStateUpdateRequest acceptRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.ACCEPTED)
            .build();

        mockMvc.perform(patch("/api/v1/bookings/" + bookingId + "/state")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ownerEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("ACCEPTED"));

        // Step 3: Owner starts booking
        BookingStateUpdateRequest startRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.IN_PROGRESS)
            .build();

        mockMvc.perform(patch("/api/v1/bookings/" + bookingId + "/state")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ownerEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(startRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("IN_PROGRESS"));

        // Step 4: Owner marks as returned
        BookingStateUpdateRequest returnRequest = BookingStateUpdateRequest.builder()
            .state(BookingState.RETURNED)
            .build();

        mockMvc.perform(patch("/api/v1/bookings/" + bookingId + "/state")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ownerEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(returnRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("RETURNED"));
    }

    private static org.assertj.core.api.AbstractObjectAssert<?, ?> assertThat(BookingState state) {
        return org.assertj.core.api.Assertions.assertThat(state);
    }
}
