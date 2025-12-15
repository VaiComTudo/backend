package com.vaicomtudo.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaicomtudo.backend.data.entity.Booking;
import com.vaicomtudo.backend.data.entity.BookingState;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.User;
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

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, ListingRepository listingRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String renterEmail) {
        // Validate dates
        if (request.getDropoffDateTime().isBefore(request.getPickupDateTime()) || 
            request.getDropoffDateTime().isEqual(request.getPickupDateTime())) {
            throw new InvalidBookingDatesException();
        }

        // Fetch renter
        User renter = userRepository.findByAccountEmail(renterEmail)
            .orElseThrow(EmailNotFoundException::new);

        // Fetch listing
        Listing listing = listingRepository.findById(request.getListingId())
            .orElseThrow(ListingNotFoundException::new);

        // Prevent renters from booking their own listings
        if (listing.getOwner().getId().equals(renter.getId())) {
            throw new UnauthorizedBookingAccessException("You cannot book your own listing");
        }

        // Create booking
        Booking booking = Booking.builder()
            .renter(renter)
            .listing(listing)
            .pickupDateTime(request.getPickupDateTime())
            .dropoffDateTime(request.getDropoffDateTime())
            .state(BookingState.REQUESTED)
            .build();

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking);
    }

    public List<BookingResponse> getRenterBookings(String renterEmail) {
        User renter = userRepository.findByAccountEmail(renterEmail)
            .orElseThrow(EmailNotFoundException::new);

        List<Booking> bookings = bookingRepository.findByRenter(renter);
        return bookings.stream()
            .map(this::toBookingResponse)
            .collect(Collectors.toList());
    }

    public List<BookingResponse> getOwnerBookings(String ownerEmail) {
        User owner = userRepository.findByAccountEmail(ownerEmail)
            .orElseThrow(EmailNotFoundException::new);

        List<Booking> bookings = bookingRepository.findByListingOwner(owner);
        return bookings.stream()
            .map(this::toBookingResponse)
            .collect(Collectors.toList());
    }

    public BookingResponse getBooking(UUID bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(BookingNotFoundException::new);

        // Verify user is either renter or owner
        if (!booking.getRenter().getAccount().getEmail().equals(userEmail) &&
            !booking.getListing().getOwner().getAccount().getEmail().equals(userEmail)) {
            throw new UnauthorizedBookingAccessException();
        }

        return toBookingResponse(booking);
    }

    @Transactional
    public BookingResponse updateBookingState(UUID bookingId, BookingState newState, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(BookingNotFoundException::new);

        User user = userRepository.findByAccountEmail(userEmail)
            .orElseThrow(EmailNotFoundException::new);

        // Validate state transition and user authorization
        validateStateTransition(booking, newState, user);

        booking.setState(newState);
        Booking updatedBooking = bookingRepository.save(booking);
        return toBookingResponse(updatedBooking);
    }

    @Transactional
    public void cancelBooking(UUID bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(BookingNotFoundException::new);

        // Verify user is either renter or owner
        if (!booking.getRenter().getAccount().getEmail().equals(userEmail) &&
            !booking.getListing().getOwner().getAccount().getEmail().equals(userEmail)) {
            throw new UnauthorizedBookingAccessException();
        }

        // Can only cancel if booking is in REQUESTED or ACCEPTED state
        if (booking.getState() != BookingState.REQUESTED && booking.getState() != BookingState.ACCEPTED) {
            throw new InvalidBookingStateTransitionException("Can only cancel bookings in REQUESTED or ACCEPTED state");
        }

        booking.setState(BookingState.CANCELLED);
        bookingRepository.save(booking);
    }

    private void validateStateTransition(Booking booking, BookingState newState, User user) {
        BookingState currentState = booking.getState();
        boolean isOwner = booking.getListing().getOwner().getId().equals(user.getId());

        switch (newState) {
            case ACCEPTED:
            case DENIED:
                // Only owner can accept or deny from REQUESTED state
                if (!isOwner) {
                    throw new UnauthorizedBookingAccessException("Only the owner can accept or deny bookings");
                }
                if (currentState != BookingState.REQUESTED) {
                    throw new InvalidBookingStateTransitionException("Can only accept or deny REQUESTED bookings");
                }
                break;

            case IN_PROGRESS:
                // Only owner can mark as in progress, must be in ACCEPTED state
                if (!isOwner) {
                    throw new UnauthorizedBookingAccessException("Only the owner can start the booking");
                }
                if (currentState != BookingState.ACCEPTED) {
                    throw new InvalidBookingStateTransitionException("Can only start bookings that are ACCEPTED");
                }
                break;

            case RETURNED:
                // Only owner can mark as returned, must be IN_PROGRESS
                if (!isOwner) {
                    throw new UnauthorizedBookingAccessException("Only the owner can mark bookings as RETURNED");
                }
                if (currentState != BookingState.IN_PROGRESS) {
                    throw new InvalidBookingStateTransitionException("Can only mark IN_PROGRESS bookings as RETURNED");
                }
                break;

            case STOLEN:
                // Only owner can report as stolen, must be IN_PROGRESS
                if (!isOwner) {
                    throw new UnauthorizedBookingAccessException("Only the owner can report bookings as STOLEN");
                }
                if (currentState != BookingState.IN_PROGRESS) {
                    throw new InvalidBookingStateTransitionException("Can only report IN_PROGRESS bookings as STOLEN");
                }
                break;

            case CANCELLED:
                // Handled separately in cancelBooking method
                throw new InvalidBookingStateTransitionException("Use cancel endpoint to cancel bookings");

            case REQUESTED:
                // Cannot transition back to REQUESTED
                throw new InvalidBookingStateTransitionException("Cannot transition back to REQUESTED state");

            default:
                throw new InvalidBookingStateTransitionException("Invalid state transition");
        }
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
            .id(booking.getId())
            .listingId(booking.getListing().getId())
            .listingTitle(booking.getListing().getTitle())
            .renterId(booking.getRenter().getId())
            .renterName(booking.getRenter().getAccount().getName())
            .ownerId(booking.getListing().getOwner().getId())
            .ownerName(booking.getListing().getOwner().getAccount().getName())
            .pickupDateTime(booking.getPickupDateTime())
            .dropoffDateTime(booking.getDropoffDateTime())
            .state(booking.getState())
            .build();
    }
}
