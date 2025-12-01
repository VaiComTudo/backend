package com.vaicomtudo.backend.data.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    @Query("SELECT l FROM Listing l WHERE l.state = :state AND l.vehicle.type = :type")
    List<Listing> findByStateAndVehicleType(@Param("state") ListingState state, @Param("type") String type);
}