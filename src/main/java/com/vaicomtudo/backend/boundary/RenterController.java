package com.vaicomtudo.backend.boundary;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.service.ListingService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/renters")
public class RenterController {

    private ListingService listingService;

    public RenterController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/listings")
    @PreAuthorize("hasRole('NORMAL_USER')")
    public ResponseEntity<List<Listing>> getAvailableListingsByCategory(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {

        // Se ambos os filtros de categoria e localização estão presentes
        if (category != null && !category.isEmpty() && location != null && !location.isEmpty()) {
            List<Listing> listings = listingService.getAvailableListingsByCategoryAndLocation(category, location);
            return ResponseEntity.ok(listings);
        }

        // Se apenas a categoria está presente
        if (category != null && !category.isEmpty()) {
            List<Listing> listings = listingService.getAvailableListingsByCategory(category);
            return ResponseEntity.ok(listings);
        }

        // Se apenas a localização está presente
        if (location != null && !location.isEmpty()) {
            List<Listing> listings = listingService.getAvailableListingsByLocation(location);
            return ResponseEntity.ok(listings);
        }

        // Se não houver filtros, retorna todos os listings disponíveis
        List<Listing> listings = listingService.getAllAvailableListings();
        return ResponseEntity.ok(listings);
    }
}
