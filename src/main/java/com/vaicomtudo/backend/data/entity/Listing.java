
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
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @Getter
    @Setter
    private User owner;

    @Column(nullable = false)
    @Getter
    @Setter
    private String description;

    @Column(nullable = false)
    @Getter
    @Setter
    private String title;

    @Column(nullable = false, precision = 19, scale = 2)
    @Getter
    @Setter
    private BigDecimal price;

    @Column(nullable = false)
    @Getter
    @Setter
    private ListingState state = ListingState.AVAILABLE;

    @ElementCollection
    @CollectionTable(
        name = "listing_availability",
        joinColumns = @JoinColumn(name = "listing_id")
    )
    private Set<AvailabilityPeriod> availability = new HashSet<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    private List<ListingPhoto> photos = new ArrayList<>();
}