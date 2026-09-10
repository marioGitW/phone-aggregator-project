package finki.ukim.mk.phone_aggregator.dto;

import java.util.List;

/**
 * One store's listing of a phone model, covering every color it carries - one entry per
 * (model, source) rather than one per color. minPrice/maxPrice span the color variants;
 * availableColors is the deduplicated set of colorCanonical values, for a quick summary;
 * colors carries the actual per-color listings (link, price, raw/canonical color).
 * phoneModelId is repeated on every group (same value throughout, since one call is
 * always scoped to one model) so the frontend can call GET /api/models/{id}/price-history
 * without a separate lookup.
 */
public record ProductOfferDto(
        Long phoneModelId,
        String brand,
        String title,
        String source,
        Integer minPrice,
        Integer maxPrice,
        List<String> availableColors,
        List<ColorOfferDto> colors
) {
}
