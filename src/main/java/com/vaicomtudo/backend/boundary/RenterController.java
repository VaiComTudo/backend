package com.vaicomtudo.backend.boundary;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.service.ListingService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/renters")
@CrossOrigin(origins = "*")
public class RenterController {

    private ListingService listingService;

    public RenterController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/listings")
    public ResponseEntity<List<Listing>> getAvailableListingsByCategory(
            @RequestParam(required = false) String category) {

        if (category != null && !category.isEmpty()) {
            List<Listing> listings = listingService.getAvailableListingsByCategory(category);
            return ResponseEntity.ok(listings);
        }

        // Se não houver categoria, retorna todos os listings disponíveis
        List<Listing> listings = listingService.getAllAvailableListings();
        return ResponseEntity.ok(listings);
    }
}
