package com.vaicomtudo.backend.data.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByStateAndVehicleType(ListingState state, String type);

    List<Listing> findByState(ListingState state);

    List<Listing> findByStateAndPickUpLocationContainingIgnoreCase(ListingState state, String location);

    List<Listing> findByStateAndDropOffLocationContainingIgnoreCase(ListingState state, String location);

    List<Listing> findByStateAndVehicleTypeAndPickUpLocationContainingIgnoreCase(ListingState state, String type,
            String location);

    List<Listing> findByStateAndVehicleTypeAndDropOffLocationContainingIgnoreCase(ListingState state, String type,
            String location);
}