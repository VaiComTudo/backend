package com.vaicomtudo.backend.data.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;
import com.vaicomtudo.backend.data.entity.User;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {
    Page<Listing> findByOwner(User owner, Pageable pageable);
    
    @Query("SELECT l FROM Listing l WHERE l.owner = :owner AND l.state != :invalidState")
    Page<Listing> findByOwnerExcludingInvalid(@Param("owner") User owner, @Param("invalidState") ListingState invalidState, Pageable pageable);
}