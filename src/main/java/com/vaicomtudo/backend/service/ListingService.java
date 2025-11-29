package com.vaicomtudo.backend.service;

import java.util.Set;
import java.util.UUID;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.ListingRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;
import com.vaicomtudo.backend.exception.MismatchIDException;

public class ListingService {

    private ListingRepository listingRepository;
    private UserRepository userRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    public Listing saveListing(Listing listing, UUID id) {
        if (!listing.getOwner().getId().equals(id)) {
            throw new MismatchIDException();
        }
        if (userRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("UUID not found!");
        }

        return listingRepository.save(listing);
    }

    public Set<Listing> getListings(UUID id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("UUID not found!");
        }

        User user = userRepository.findById(id).get();

        return user.getListings();
    }
}