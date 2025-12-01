package com.vaicomtudo.backend.boundary;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.service.ListingService;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/v1/owners")
public class OwnerController {

    private ListingService listingService;

    public OwnerController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PreAuthorize("hasRole('NORMAL_USER')")
    @PostMapping("/{id}/listings")
    public ResponseEntity<Listing> addListing(@PathVariable UUID id, @RequestBody Listing listing) {
        
        Listing response = listingService.saveListing(listing, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('NORMAL_USER')")
    @GetMapping("/{id}/listings")
    public ResponseEntity<Set<Listing>> getListings(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.getListings(id));
    }
    
}