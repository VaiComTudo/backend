
package com.vaicomtudo.backend.data.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"owner", "availability", "photos"})
@Data
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonBackReference
    private User owner;

    @Column(nullable = false)
    @NotBlank
    private String description;

    @Column(nullable = false)
    @NotBlank
    private String title;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private ListingState state = ListingState.AVAILABLE;

    @Embedded
    @Column(nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    @NotBlank
    private String pickUpLocation;

    @Column(nullable = false)
    @NotBlank
    private String dropOffLocation;

    @ElementCollection
    @CollectionTable(
        name = "listing_availability",
        joinColumns = @JoinColumn(name = "listing_id")
    )
    private Set<AvailabilityPeriod> availability = new HashSet<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    private List<ListingPhoto> photos = new ArrayList<>();
}