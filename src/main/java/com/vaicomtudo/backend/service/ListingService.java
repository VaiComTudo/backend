package com.vaicomtudo.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaicomtudo.backend.data.entity.Booking;
import com.vaicomtudo.backend.data.entity.BookingState;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.BookingRepository;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.data.specification.ListingSpecification;
import com.vaicomtudo.backend.exception.EmailNotFoundException;
import com.vaicomtudo.backend.exception.ListingNotFoundException;
import com.vaicomtudo.backend.exception.MismatchEmailException;
import com.vaicomtudo.backend.exception.UnauthorizedListingAccessException;

@Service
public class ListingService {

    private ListingRepository listingRepository;
    private UserRepository userRepository;
    private BookingRepository bookingRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository, BookingRepository bookingRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    public Listing saveListing(Listing listing, String email) {
        // Check if owner is already set and if it matches the id parameter
        if (listing.getOwner() != null && !listing.getOwner().getAccount().getEmail().equals(email)) {
            throw new MismatchEmailException();
        }

        // Fetch the user from the database
        User owner = userRepository.findByAccountEmail(email)
            .orElseThrow(EmailNotFoundException::new);

        // Use the convenience helper method to maintain bidirectional relationship
        owner.addListing(listing);

        return listingRepository.save(listing);
    }

    public Set<Listing> getListings(String email) {
        User user = userRepository.findByAccountEmail(email)
            .orElseThrow(EmailNotFoundException::new);

        return user.getListings().stream()
            .filter(listing -> listing.getState() != ListingState.INVALID)
            .collect(java.util.stream.Collectors.toSet());
    }

    public Page<Listing> getListingsPaginated(String email, Pageable pageable) {
        User user = userRepository.findByAccountEmail(email)
            .orElseThrow(EmailNotFoundException::new);

        return listingRepository.findByOwnerExcludingInvalid(user, ListingState.INVALID, pageable);
    }

    public Page<Listing> searchAvailableListings(
        String category,
        String location,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String currentUserEmail,
        Pageable pageable
    ) {
        Specification<Listing> spec = Specification.where(ListingSpecification.hasState(ListingState.AVAILABLE))
                .and(ListingSpecification.hasVehicleType(category))
                .and(ListingSpecification.hasLocation(location))
                .and(ListingSpecification.hasPriceGreaterThanOrEqual(minPrice))
                .and(ListingSpecification.hasPriceLessThanOrEqual(maxPrice))
                .and(ListingSpecification.notOwnedBy(currentUserEmail));

        return listingRepository.findAll(spec, pageable);
    }

    public Listing getListingById(UUID listingId) {
        return listingRepository.findById(listingId)
            .orElseThrow(ListingNotFoundException::new);
    }

    @Transactional
    public void removeListing(UUID listingId, String email) {
        // Fetch the listing from the database
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(ListingNotFoundException::new);
        
        // Verify that the user is the owner of the listing
        if (listing.getOwner() == null || 
            !listing.getOwner().getAccount().getEmail().equals(email)) {
            throw new UnauthorizedListingAccessException();
        }
        
        // Cancel REQUESTED and ACCEPTED bookings for this listing
        List<Booking> bookings = bookingRepository.findByListing(listing);
        for (Booking booking : bookings) {
            if (booking.getState() == BookingState.REQUESTED || 
                booking.getState() == BookingState.ACCEPTED) {
                booking.setState(BookingState.CANCELLED);
                bookingRepository.save(booking);
            }
        }
        
        // Mark the listing as INVALID (soft delete)
        listing.setState(ListingState.INVALID);
        listingRepository.save(listing);
    }
}