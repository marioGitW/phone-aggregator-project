package finki.ukim.mk.phone_aggregator.specification;

import finki.ukim.mk.phone_aggregator.dto.PhoneFilterDto;
import finki.ukim.mk.phone_aggregator.model.OfferListing;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for dynamic queries against the offer_listings view (every active
 * offer joined to its phone model and latest price).
 * Converts PhoneFilterDto into database WHERE clauses.
 */
public class OfferListingSpecifications {

    /**
     * Build a specification from filter DTO
     * Combines all active filters with AND logic
     */
    public static Specification<OfferListing> fromFilter(PhoneFilterDto filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filters == null) {
                return criteriaBuilder.conjunction();
            }

            // Search filter: case-insensitive search in title and rawTitle
            if (filters.getSearch() != null && !filters.getSearch().isEmpty()) {
                String searchTerm = "%" + filters.getSearch().toLowerCase() + "%";
                predicates.add(
                    criteriaBuilder.or(
                        criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            searchTerm
                        ),
                        criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("rawTitle")),
                            searchTerm
                        )
                    )
                );
            }

            // Brand filter: IN list
            if (filters.getBrands() != null && !filters.getBrands().isEmpty()) {
                predicates.add(root.get("brand").in(filters.getBrands()));
            }

            // Source filter: IN list
            if (filters.getSources() != null && !filters.getSources().isEmpty()) {
                predicates.add(root.get("source").in(filters.getSources()));
            }

            // Minimum price filter
            if (filters.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filters.getMinPrice()));
            }

            // Maximum price filter
            if (filters.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filters.getMaxPrice()));
            }

            // Combine all predicates with AND
            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Search filter: case-insensitive search in title and rawTitle
     */
    public static Specification<OfferListing> searchByTitle(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchTerm = "%" + search.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchTerm),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("rawTitle")), searchTerm)
            );
        };
    }

    /**
     * Filter by brand names (IN list)
     */
    public static Specification<OfferListing> filterByBrands(List<String> brands) {
        return (root, query, criteriaBuilder) -> {
            if (brands == null || brands.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("brand").in(brands);
        };
    }

    /**
     * Filter by source names (IN list)
     */
    public static Specification<OfferListing> filterBySources(List<String> sources) {
        return (root, query, criteriaBuilder) -> {
            if (sources == null || sources.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("source").in(sources);
        };
    }

    /**
     * Filter by price range
     */
    public static Specification<OfferListing> filterByPriceRange(Integer minPrice, Integer maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
