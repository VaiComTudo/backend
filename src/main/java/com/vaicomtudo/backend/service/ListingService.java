package com.vaicomtudo.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    public List<Listing> getAvailableListingsByLocation(String location) {
        // Duas coleções diferentes, uma para pickup e outra para drop off
        List<Listing> pickUpResults = listingRepository.findByStateAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location);
        List<Listing> dropOffResults = listingRepository.findByStateAndDropOffLocationContainingIgnoreCase(
                ListingState.AVAILABLE, location);

        // Combinar os resultados e remover duplicados usando LinkedHashSet para
        // preservar a ordem
        Set<Listing> combinedSet = new LinkedHashSet<>(pickUpResults);
        combinedSet.addAll(dropOffResults);

        return new ArrayList<>(combinedSet);
    }

    public List<Listing> getAvailableListingsByCategoryAndLocation(String category, String location) {
        // Lógica igual ao getAvailableListingsByLocation, mas com categoria e
        // localização combinadas
        List<Listing> pickUpResults = listingRepository.findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(
                ListingState.AVAILABLE, category, location);
        List<Listing> dropOffResults = listingRepository
                .findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(
                        ListingState.AVAILABLE, category, location);

        Set<Listing> combinedSet = new LinkedHashSet<>(pickUpResults);
        combinedSet.addAll(dropOffResults);

        return new ArrayList<>(combinedSet);
    }
}