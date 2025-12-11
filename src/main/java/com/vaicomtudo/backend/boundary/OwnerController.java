package com.vaicomtudo.backend.boundary;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.service.ListingService;

import io.micrometer.core.annotation.Timed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/v1/owners")
public class OwnerController {

    private ListingService listingService;

    public OwnerController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PreAuthorize("hasRole('NORMAL_USER')")
    @PostMapping("/listings")
    @Timed(value = "request.ownerspostlistings")
    public ResponseEntity<Listing> addListing(@RequestBody Listing listing, Authentication authentication) {
        String email = authentication.getName();
        Listing response = listingService.saveListing(listing, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('NORMAL_USER')")
    @GetMapping("/listings")
    @Timed(value = "request.ownersgetlistings")
    public ResponseEntity<Page<Listing>> getListings(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        String email = authentication.getName();

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(listingService.getListingsPaginated(email, pageable));
    }

}