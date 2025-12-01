package com.vaicomtudo.backend.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.exception.IDNotFoundException;
import com.vaicomtudo.backend.exception.MismatchIDException;

@Service
public class ListingService {

    private ListingRepository listingRepository;
    private UserRepository userRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    public Listing saveListing(Listing listing, UUID id) {
        // Check if owner is already set and if it matches the id parameter
        if (listing.getOwner() != null && !listing.getOwner().getId().equals(id)) {
            throw new MismatchIDException();
        }

        // Fetch the user from the database
        User owner = userRepository.findById(id)
            .orElseThrow(() -> new IDNotFoundException());

        // Use the convenience helper method to maintain bidirectional relationship
        owner.addListing(listing);

        return listingRepository.save(listing);
    }

    public Set<Listing> getListings(UUID id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new IDNotFoundException();
        }

        User user = userRepository.findById(id).get();

        return user.getListings();
    }
}