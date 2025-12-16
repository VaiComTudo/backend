package com.vaicomtudo.backend.data.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vaicomtudo.backend.data.entity.Booking;
import com.vaicomtudo.backend.data.entity.BookingState;
import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByRenter(User renter);
    List<Booking> findByListing(Listing listing);
    List<Booking> findByListingOwner(User owner);
    List<Booking> findByState(BookingState state);
    List<Booking> findByRenterAndState(User renter, BookingState state);
    List<Booking> findByListingOwnerAndState(User owner, BookingState state);
}
