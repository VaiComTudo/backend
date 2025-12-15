package com.vaicomtudo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Booking;
import com.vaicomtudo.backend.data.entity.BookingState;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.entity.Vehicle;
import com.vaicomtudo.backend.data.entity.VehicleCondition;
import com.vaicomtudo.backend.data.repository.BookingRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.dto.BookingRequest;
import com.vaicomtudo.backend.dto.BookingResponse;
import com.vaicomtudo.backend.exception.BookingNotFoundException;
import com.vaicomtudo.backend.exception.EmailNotFoundException;
import com.vaicomtudo.backend.exception.InvalidBookingDatesException;
import com.vaicomtudo.backend.exception.InvalidBookingStateTransitionException;
import com.vaicomtudo.backend.exception.ListingNotFoundException;
import com.vaicomtudo.backend.exception.UnauthorizedBookingAccessException;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User renter;
    private User owner;
    private Listing listing;
    private Booking booking;
    private String renterEmail;
    private String ownerEmail;
    private LocalDateTime pickupDate;
    private LocalDateTime dropoffDate;

    @BeforeEach
    void setUp() {
        renterEmail = "renter@email.com";
        ownerEmail = "owner@email.com";
        pickupDate = LocalDateTime.now().plusDays(7);
        dropoffDate = LocalDateTime.now().plusDays(9);

        // Setup renter
        Account renterAccount = new Account();
        renterAccount.setId(UUID.randomUUID());
        renterAccount.setEmail(renterEmail);
        renterAccount.setName("Renter User");

        renter = new User();
        renter.setId(UUID.randomUUID());
        renter.setAccount(renterAccount);
        renter.setBirthdate(LocalDate.of(1995, 5, 15));

        // Setup owner
        Account ownerAccount = new Account();
        ownerAccount.setId(UUID.randomUUID());
        ownerAccount.setEmail(ownerEmail);
        ownerAccount.setName("Owner User");

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setAccount(ownerAccount);
        owner.setBirthdate(LocalDate.of(1990, 1, 1));

        // Setup listing
        listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setOwner(owner);
        listing.setTitle("Electric Scooter");
        listing.setDescription("Fast electric scooter");
        listing.setPrice(BigDecimal.valueOf(25.00));
        listing.setState(ListingState.AVAILABLE);

        Vehicle vehicle = new Vehicle();
        vehicle.setType("scooter");
        vehicle.setCondition(VehicleCondition.GOOD);
        listing.setVehicle(vehicle);
        listing.setPickUpLocation("Downtown");
        listing.setDropOffLocation("Downtown");

        // Setup booking
        booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setRenter(renter);
        booking.setListing(listing);
        booking.setPickupDateTime(pickupDate);
        booking.setDropoffDateTime(dropoffDate);
        booking.setState(BookingState.REQUESTED);
    }

    @Test
    @DisplayName("createBooking should create booking when valid request")
    @Requirement("VCT-36")
    void whenCreateBooking_withValidRequest_thenBookingCreated() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        when(userRepository.findByAccountEmail(renterEmail)).thenReturn(Optional.of(renter));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act
        BookingResponse response = bookingService.createBooking(request, renterEmail);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(booking.getId());
        assertThat(response.getListingId()).isEqualTo(listing.getId());
        assertThat(response.getRenterId()).isEqualTo(renter.getId());
        assertThat(response.getOwnerId()).isEqualTo(owner.getId());
        assertThat(response.getState()).isEqualTo(BookingState.REQUESTED);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking should throw exception when dropoff before pickup")
    @Requirement("VCT-36")
    void whenCreateBooking_withDropoffBeforePickup_thenThrowException() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(dropoffDate)
            .dropoffDateTime(pickupDate)
            .build();

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, renterEmail))
            .isInstanceOf(InvalidBookingDatesException.class);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking should throw exception when dates are equal")
    @Requirement("VCT-36")
    void whenCreateBooking_withEqualDates_thenThrowException() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(pickupDate)
            .build();

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, renterEmail))
            .isInstanceOf(InvalidBookingDatesException.class);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking should throw exception when renter not found")
    @Requirement("VCT-36")
    void whenCreateBooking_withNonexistentRenter_thenThrowException() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        when(userRepository.findByAccountEmail(renterEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, renterEmail))
            .isInstanceOf(EmailNotFoundException.class);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking should throw exception when listing not found")
    @Requirement("VCT-36")
    void whenCreateBooking_withNonexistentListing_thenThrowException() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        when(userRepository.findByAccountEmail(renterEmail)).thenReturn(Optional.of(renter));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, renterEmail))
            .isInstanceOf(ListingNotFoundException.class);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking should throw exception when renter tries to book own listing")
    @Requirement("VCT-36")
    void whenCreateBooking_withRenterAsOwner_thenThrowException() {
        // Arrange
        listing.setOwner(renter);

        BookingRequest request = BookingRequest.builder()
            .listingId(listing.getId())
            .pickupDateTime(pickupDate)
            .dropoffDateTime(dropoffDate)
            .build();

        when(userRepository.findByAccountEmail(renterEmail)).thenReturn(Optional.of(renter));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, renterEmail))
            .isInstanceOf(UnauthorizedBookingAccessException.class)
            .hasMessageContaining("cannot book your own listing");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("getRenterBookings should return list of bookings for renter")
    @Requirement("VCT-36")
    void whenGetRenterBookings_thenReturnBookingsList() {
        // Arrange
        when(userRepository.findByAccountEmail(renterEmail)).thenReturn(Optional.of(renter));
        when(bookingRepository.findByRenter(renter)).thenReturn(List.of(booking));

        // Act
        List<BookingResponse> responses = bookingService.getRenterBookings(renterEmail);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRenterId()).isEqualTo(renter.getId());
        verify(bookingRepository, times(1)).findByRenter(renter);
    }

    @Test
    @DisplayName("getOwnerBookings should return list of bookings for owner")
    @Requirement("VCT-36")
    void whenGetOwnerBookings_thenReturnBookingsList() {
        // Arrange
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByListingOwner(owner)).thenReturn(List.of(booking));

        // Act
        List<BookingResponse> responses = bookingService.getOwnerBookings(ownerEmail);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOwnerId()).isEqualTo(owner.getId());
        verify(bookingRepository, times(1)).findByListingOwner(owner);
    }

    @Test
    @DisplayName("getBooking should return booking when user is renter")
    @Requirement("VCT-36")
    void whenGetBooking_asRenter_thenReturnBooking() {
        // Arrange
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Act
        BookingResponse response = bookingService.getBooking(booking.getId(), renterEmail);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(booking.getId());
    }

    @Test
    @DisplayName("getBooking should return booking when user is owner")
    @Requirement("VCT-36")
    void whenGetBooking_asOwner_thenReturnBooking() {
        // Arrange
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Act
        BookingResponse response = bookingService.getBooking(booking.getId(), ownerEmail);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(booking.getId());
    }

    @Test
    @DisplayName("getBooking should throw exception when user is neither renter nor owner")
    @Requirement("VCT-36")
    void whenGetBooking_asUnauthorizedUser_thenThrowException() {
        // Arrange
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.getBooking(booking.getId(), "other@email.com"))
            .isInstanceOf(UnauthorizedBookingAccessException.class);
    }

    @Test
    @DisplayName("updateBookingState should accept booking when owner accepts from REQUESTED")
    @Requirement("VCT-36")
    void whenUpdateBookingState_ownerAcceptsRequested_thenBookingAccepted() {
        // Arrange
        booking.setState(BookingState.REQUESTED);
        Booking acceptedBooking = new Booking();
        acceptedBooking.setId(booking.getId());
        acceptedBooking.setRenter(renter);
        acceptedBooking.setListing(listing);
        acceptedBooking.setPickupDateTime(pickupDate);
        acceptedBooking.setDropoffDateTime(dropoffDate);
        acceptedBooking.setState(BookingState.ACCEPTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any(Booking.class))).thenReturn(acceptedBooking);

        // Act
        BookingResponse response = bookingService.updateBookingState(booking.getId(), BookingState.ACCEPTED, ownerEmail);

        // Assert
        assertThat(response.getState()).isEqualTo(BookingState.ACCEPTED);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingState should deny booking when owner denies from REQUESTED")
    @Requirement("VCT-36")
    void whenUpdateBookingState_ownerDeniesRequested_thenBookingDenied() {
        // Arrange
        booking.setState(BookingState.REQUESTED);
        Booking deniedBooking = new Booking();
        deniedBooking.setId(booking.getId());
        deniedBooking.setRenter(renter);
        deniedBooking.setListing(listing);
        deniedBooking.setPickupDateTime(pickupDate);
        deniedBooking.setDropoffDateTime(dropoffDate);
        deniedBooking.setState(BookingState.DENIED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any(Booking.class))).thenReturn(deniedBooking);

        // Act
        BookingResponse response = bookingService.updateBookingState(booking.getId(), BookingState.DENIED, ownerEmail);

        // Assert
        assertThat(response.getState()).isEqualTo(BookingState.DENIED);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingState should throw exception when renter tries to accept")
    @Requirement("VCT-36")
    void whenUpdateBookingState_renterTriesToAccept_thenThrowException() {
        // Arrange
        booking.setState(BookingState.REQUESTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(renterEmail)).thenReturn(Optional.of(renter));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.updateBookingState(booking.getId(), BookingState.ACCEPTED, renterEmail))
            .isInstanceOf(UnauthorizedBookingAccessException.class)
            .hasMessageContaining("Only the owner");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingState should start booking when owner sets to IN_PROGRESS from ACCEPTED")
    @Requirement("VCT-36")
    void whenUpdateBookingState_ownerStartsAccepted_thenBookingInProgress() {
        // Arrange
        booking.setState(BookingState.ACCEPTED);
        Booking inProgressBooking = new Booking();
        inProgressBooking.setId(booking.getId());
        inProgressBooking.setRenter(renter);
        inProgressBooking.setListing(listing);
        inProgressBooking.setPickupDateTime(pickupDate);
        inProgressBooking.setDropoffDateTime(dropoffDate);
        inProgressBooking.setState(BookingState.IN_PROGRESS);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any(Booking.class))).thenReturn(inProgressBooking);

        // Act
        BookingResponse response = bookingService.updateBookingState(booking.getId(), BookingState.IN_PROGRESS, ownerEmail);

        // Assert
        assertThat(response.getState()).isEqualTo(BookingState.IN_PROGRESS);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingState should throw exception when trying to start from REQUESTED")
    @Requirement("VCT-36")
    void whenUpdateBookingState_startFromRequested_thenThrowException() {
        // Arrange
        booking.setState(BookingState.REQUESTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.updateBookingState(booking.getId(), BookingState.IN_PROGRESS, ownerEmail))
            .isInstanceOf(InvalidBookingStateTransitionException.class)
            .hasMessageContaining("ACCEPTED");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingState should return booking when owner sets to RETURNED from IN_PROGRESS")
    @Requirement("VCT-36")
    void whenUpdateBookingState_ownerReturnsInProgress_thenBookingReturned() {
        // Arrange
        booking.setState(BookingState.IN_PROGRESS);
        Booking returnedBooking = new Booking();
        returnedBooking.setId(booking.getId());
        returnedBooking.setRenter(renter);
        returnedBooking.setListing(listing);
        returnedBooking.setPickupDateTime(pickupDate);
        returnedBooking.setDropoffDateTime(dropoffDate);
        returnedBooking.setState(BookingState.RETURNED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any(Booking.class))).thenReturn(returnedBooking);

        // Act
        BookingResponse response = bookingService.updateBookingState(booking.getId(), BookingState.RETURNED, ownerEmail);

        // Assert
        assertThat(response.getState()).isEqualTo(BookingState.RETURNED);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingState should report stolen when owner sets to STOLEN from IN_PROGRESS")
    @Requirement("VCT-36")
    void whenUpdateBookingState_ownerReportsStolenFromInProgress_thenBookingStolen() {
        // Arrange
        booking.setState(BookingState.IN_PROGRESS);
        Booking stolenBooking = new Booking();
        stolenBooking.setId(booking.getId());
        stolenBooking.setRenter(renter);
        stolenBooking.setListing(listing);
        stolenBooking.setPickupDateTime(pickupDate);
        stolenBooking.setDropoffDateTime(dropoffDate);
        stolenBooking.setState(BookingState.STOLEN);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any(Booking.class))).thenReturn(stolenBooking);

        // Act
        BookingResponse response = bookingService.updateBookingState(booking.getId(), BookingState.STOLEN, ownerEmail);

        // Assert
        assertThat(response.getState()).isEqualTo(BookingState.STOLEN);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingState should throw exception when trying to return from REQUESTED")
    @Requirement("VCT-36")
    void whenUpdateBookingState_returnFromRequested_thenThrowException() {
        // Arrange
        booking.setState(BookingState.REQUESTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByAccountEmail(ownerEmail)).thenReturn(Optional.of(owner));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.updateBookingState(booking.getId(), BookingState.RETURNED, ownerEmail))
            .isInstanceOf(InvalidBookingStateTransitionException.class)
            .hasMessageContaining("IN_PROGRESS");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("cancelBooking should cancel booking when renter cancels from REQUESTED")
    @Requirement("VCT-36")
    void whenCancelBooking_renterCancelsRequested_thenBookingCancelled() {
        // Arrange
        booking.setState(BookingState.REQUESTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Act
        bookingService.cancelBooking(booking.getId(), renterEmail);

        // Assert
        assertThat(booking.getState()).isEqualTo(BookingState.CANCELLED);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking should cancel when owner cancels from REQUESTED")
    @Requirement("VCT-XX")
    void whenCancelBooking_ownerCancelsRequested_thenBookingCancelled() {
        // Arrange
        booking.setState(BookingState.REQUESTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Act
        bookingService.cancelBooking(booking.getId(), ownerEmail);

        // Assert
        assertThat(booking.getState()).isEqualTo(BookingState.CANCELLED);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking should cancel when from ACCEPTED state")
    @Requirement("VCT-XX")
    void whenCancelBooking_fromAccepted_thenBookingCancelled() {
        // Arrange
        booking.setState(BookingState.ACCEPTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Act
        bookingService.cancelBooking(booking.getId(), renterEmail);

        // Assert
        assertThat(booking.getState()).isEqualTo(BookingState.CANCELLED);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking should throw exception when booking not found")
    @Requirement("VCT-36")
    void whenCancelBooking_bookingNotFound_thenThrowException() {
        // Arrange
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(booking.getId(), renterEmail))
            .isInstanceOf(BookingNotFoundException.class);
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
