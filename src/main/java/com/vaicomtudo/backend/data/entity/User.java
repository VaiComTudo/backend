package com.vaicomtudo.backend.data.entity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"listings", "defaultAvailability"})
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDate birthdate;

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    // TODO: provavelmente calular isto com a lista de ratings
    @Column(nullable = false)
    private double rating;

    @ElementCollection
    @CollectionTable(
        name = "user_availability",
        joinColumns = @JoinColumn(name = "user_id")
    )
    private Set<AvailabilityPeriod> defaultAvailability = new HashSet<>();


    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Listing> listings = new HashSet<>();

    // convenience helper
    public void addListing(Listing listing) {
        listings.add(listing);
        listing.setOwner(this);
    }

    public void removeListing(Listing listing) {
        listings.remove(listing);
        listing.setOwner(null);
    }
}