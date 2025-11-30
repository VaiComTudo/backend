package com.vaicomtudo.backend.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
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
        if (!listing.getOwner().getId().equals(id)) {
            throw new MismatchIDException();
        }
        if (userRepository.findById(id).isEmpty()) {
            throw new IDNotFoundException();
        }

        return listingRepository.save(listing);
    }

    public Set<Listing> getListings(UUID id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new IDNotFoundException();
        }

        User user = userRepository.findById(id).get();

        return user.getListings();
    }

    public List<Listing> getAvailableListingsByCategory(String category) {
        return listingRepository.findByStateAndVehicleType(ListingState.AVAILABLE, category);
    }
}