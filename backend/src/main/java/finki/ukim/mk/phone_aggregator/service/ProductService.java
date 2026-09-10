package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.dto.ColorOfferDto;
import finki.ukim.mk.phone_aggregator.dto.ProductOfferDto;
import finki.ukim.mk.phone_aggregator.model.OfferListing;
import finki.ukim.mk.phone_aggregator.repository.OfferListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProductService {

    private final OfferListingRepository offerListingRepository;

    public ProductService(OfferListingRepository offerListingRepository) {
        this.offerListingRepository = offerListingRepository;
    }

    /**
     * Every store currently selling the same phone model as the given offer, one entry
     * per (model, source) rather than one per color - offerListingRepository is already
     * scoped to isActive offers (see the offer_listings view), so a discontinued listing
     * never shows up here. Offers sharing a phoneModel are the true duplicates now, so
     * unlike the old fuzzy-title matching this is an exact join.
     */
    public List<ProductOfferDto> getOffers(Long offerId) {
        OfferListing base = offerListingRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Offer not found"));

        Map<String, List<OfferListing>> bySource = offerListingRepository.findByPhoneModelId(base.getPhoneModelId())
                .stream()
                .sorted(Comparator.comparing(OfferListing::getSource))
                .collect(Collectors.groupingBy(OfferListing::getSource, LinkedHashMap::new, Collectors.toList()));

        return bySource.values().stream()
                .map(this::toGroupDto)
                .toList();
    }

    private ProductOfferDto toGroupDto(List<OfferListing> offersForSource) {
        OfferListing first = offersForSource.get(0);

        int minPrice = offersForSource.stream().mapToInt(OfferListing::getPrice).min().orElseThrow();
        int maxPrice = offersForSource.stream().mapToInt(OfferListing::getPrice).max().orElseThrow();

        List<String> availableColors = offersForSource.stream()
                .map(OfferListing::getColorCanonical)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        List<ColorOfferDto> colors = offersForSource.stream()
                .sorted(Comparator.comparing(OfferListing::getPrice))
                .map(o -> new ColorOfferDto(o.getId(), o.getColorRaw(), o.getColorCanonical(), o.getPrice(), o.getSiteLink(), o.getImageUrl()))
                .toList();

        return new ProductOfferDto(
                first.getPhoneModelId(),
                first.getBrand(),
                first.getTitle(),
                first.getSource(),
                minPrice,
                maxPrice,
                availableColors,
                colors
        );
    }
}
