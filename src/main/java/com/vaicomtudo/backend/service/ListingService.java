package com.vaicomtudo.backend.service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
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

    public ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
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

        return user.getListings();
    }

    public Page<Listing> getListingsPaginated(String email, Pageable pageable) {
        User user = userRepository.findByAccountEmail(email)
            .orElseThrow(EmailNotFoundException::new);

        return listingRepository.findByOwner(user, pageable);
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

        // TODO: When booking system is implemented, cancel any pending booking requests here
        // Example: bookingService.cancelPendingBookingsForListing(listingId);
        
        // Remove the listing using the helper method to maintain bidirectional relationship
        User owner = listing.getOwner();
        owner.removeListing(listing);
        
        // Delete the listing from the repository
        listingRepository.delete(listing);
    }
}