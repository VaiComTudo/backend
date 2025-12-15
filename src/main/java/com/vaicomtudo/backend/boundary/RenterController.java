package com.vaicomtudo.backend.boundary;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.service.ListingService;

import io.micrometer.core.annotation.Timed;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/v1/renters")
public class RenterController {

    private ListingService listingService;

    public RenterController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/listings")
    @Timed(value = "request.rentersgetlistings")
    public ResponseEntity<Page<Listing>> searchListings(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "title") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = null;
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            currentUserEmail = authentication.getName();
        }

        Page<Listing> listings = listingService.searchAvailableListings(category, location, minPrice, maxPrice, currentUserEmail, pageable);
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/listings/{listingId}")
    @Timed(value = "request.rentersgetlistingbyid")
    public ResponseEntity<Listing> getListingById(@PathVariable UUID listingId) {
        Listing listing = listingService.getListingById(listingId);
        return ResponseEntity.ok(listing);
    }
}
