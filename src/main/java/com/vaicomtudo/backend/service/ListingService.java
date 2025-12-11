package com.vaicomtudo.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.exception.EmailNotFoundException;
import com.vaicomtudo.backend.exception.MismatchEmailException;

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

    public List<Listing> getAvailableListingsByCategory(String category) {
        return listingRepository.findByStateAndVehicleType(ListingState.AVAILABLE, category);
    }

    public List<Listing> getAllAvailableListings() {
        return listingRepository.findByState(ListingState.AVAILABLE);
    }
}