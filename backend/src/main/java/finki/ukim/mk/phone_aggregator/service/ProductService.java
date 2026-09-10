package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.dto.ProductOfferDto;
import finki.ukim.mk.phone_aggregator.model.OfferListing;
import finki.ukim.mk.phone_aggregator.repository.OfferListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProductService {

    private final OfferListingRepository offerListingRepository;

    public ProductService(OfferListingRepository offerListingRepository) {
        this.offerListingRepository = offerListingRepository;
    }

    /**
     * Every active offer for the same phone model as the given offer (itself included) -
     * i.e. every shop currently selling this exact phone. Offers sharing a phoneModel are
     * the true duplicates now, so unlike the old fuzzy-title matching this is an exact join.
     */
    public List<ProductOfferDto> getOffers(Long offerId) {
        OfferListing base = offerListingRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Offer not found"));

        return offerListingRepository.findByPhoneModelId(base.getPhoneModelId()).stream()
                .map(this::convertToDto)
                .toList();
    }

    private ProductOfferDto convertToDto(OfferListing listing) {
        return new ProductOfferDto(
                listing.getId(),
                listing.getBrand(),
                listing.getTitle(),
                listing.getPrice(),
                listing.getSource(),
                listing.getSiteLink(),
                listing.getImageUrl(),
                listing.getCreatedAt()
        );
    }
}
