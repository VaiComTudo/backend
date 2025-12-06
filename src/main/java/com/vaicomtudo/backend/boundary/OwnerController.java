package com.vaicomtudo.backend.boundary;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.service.ListingService;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<Listing> addListing(@RequestBody Listing listing, Authentication authentication) {
        String email = authentication.getName();
        Listing response = listingService.saveListing(listing, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('NORMAL_USER')")
    @GetMapping("/listings")
    public ResponseEntity<Set<Listing>> getListings(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(listingService.getListings(email));
    }

}