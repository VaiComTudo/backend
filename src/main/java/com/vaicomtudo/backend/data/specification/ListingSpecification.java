package com.vaicomtudo.backend.data.specification;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.vaicomtudo.backend.data.entity.Listing;
import com.vaicomtudo.backend.data.entity.ListingState;

import jakarta.persistence.criteria.Predicate;

public class ListingSpecification {

    private ListingSpecification() {
        // Private constructor to hide the implicit public one
    }

    public static Specification<Listing> hasState(ListingState state) {
        return (root, query, criteriaBuilder) ->
            state == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("state"), state);
    }

    public static Specification<Listing> hasVehicleType(String type) {
        return (root, query, criteriaBuilder) ->
            type == null || type.isEmpty() ? criteriaBuilder.conjunction() :
            criteriaBuilder.equal(root.get("vehicle").get("type"), type);
    }

    public static Specification<Listing> hasPickUpLocation(String location) {
        return (root, query, criteriaBuilder) ->
            location == null || location.isEmpty() ? criteriaBuilder.conjunction() :
            criteriaBuilder.like(criteriaBuilder.lower(root.get("pickUpLocation")),
                "%" + location.toLowerCase() + "%");
    }

    public static Specification<Listing> hasDropOffLocation(String location) {
        return (root, query, criteriaBuilder) ->
            location == null || location.isEmpty() ? criteriaBuilder.conjunction() :
            criteriaBuilder.like(criteriaBuilder.lower(root.get("dropOffLocation")),
                "%" + location.toLowerCase() + "%");
    }

    public static Specification<Listing> hasLocation(String location) {
        return (root, query, criteriaBuilder) -> {
            if (location == null || location.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + location.toLowerCase() + "%";
            Predicate pickUpMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("pickUpLocation")), pattern);
            Predicate dropOffMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("dropOffLocation")), pattern);
            return criteriaBuilder.or(pickUpMatch, dropOffMatch);
        };
    }

    public static Specification<Listing> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) ->
            minPrice == null ? criteriaBuilder.conjunction() :
            criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Listing> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
            maxPrice == null ? criteriaBuilder.conjunction() :
            criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Listing> notOwnedBy(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.notEqual(root.get("owner").get("account").get("email"), email);
        };
    }

    public static Specification<Listing> notInvalid() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.notEqual(root.get("state"), ListingState.INVALID);
    }
}
